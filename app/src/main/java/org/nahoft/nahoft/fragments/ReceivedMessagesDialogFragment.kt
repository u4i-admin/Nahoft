package org.nahoft.nahoft.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.libsodium.jni.keys.PublicKey
import org.nahoft.codex.Encryption
import org.nahoft.nahoft.Persist
import org.nahoft.nahoft.R
import org.nahoft.nahoft.databinding.FragmentDialogReceivedMessagesBinding
import org.nahoft.nahoft.models.DecryptedMessageRecord
import org.nahoft.nahoft.models.Message
import org.nahoft.nahoft.viewmodels.FriendInfoViewModel
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Dialog showing messages successfully received and decrypted during the current session.
 *
 * Each row displays the timestamp, spot count, and decrypted message content.
 * Decryption is performed on-demand on the IO dispatcher — plaintext is never
 * stored in memory beyond the scope of building each row's display string.
 *
 * If the app is locked when the dialog is opened, message content is shown as
 * redacted and the user is prompted to unlock.
 */
class ReceivedMessagesDialogFragment : BottomSheetDialogFragment()
{
    private var _binding: FragmentDialogReceivedMessagesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FriendInfoViewModel by activityViewModels()
    private val uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View
    {
        _binding = FragmentDialogReceivedMessagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        binding.btnClose.setOnClickListener { dismissAllowingStateLoss() }

        val isMfsk   = arguments?.getBoolean(ARG_IS_MFSK, false) ?: false
        val records  = if (isMfsk)
            viewModel.mfskDecryptedMessageRecords.value
        else
            viewModel.wsprDecryptedMessageRecords.value

        val friendName   = viewModel.friend.value?.name
        val publicKeyBytes = viewModel.friend.value?.publicKeyEncoded

        if (records.isEmpty())
        {
            binding.tvEmptyState.visibility = View.VISIBLE
            return
        }

        loadAndDisplayMessages(records, friendName, publicKeyBytes)
    }

    /**
     * For each DecryptedMessageRecord, locates the corresponding saved Message
     * by timestamp proximity, decrypts it, and adds a row to the container.
     */
    private fun loadAndDisplayMessages(
        records: List<DecryptedMessageRecord>,
        friendName: String?,
        publicKeyBytes: ByteArray?
    )
    {
        uiScope.launch {
            // Match each record to its saved Message on IO — Persist.messageList
            // access and decryption both happen off the main thread.
            val rows = withContext(Dispatchers.IO) {
                records.map { record ->
                    val message = findMatchingMessage(record, friendName)
                    buildRowData(record, message, publicKeyBytes)
                }
            }

            if (_binding == null) return@launch

            rows.forEach { rowData ->
                binding.messagesContainer.addView(buildRowView(rowData))
            }
        }
    }

    /**
     * Finds the saved Message whose timestamp is within 5 seconds of the record's
     * timestamp, scoped to this friend's incoming messages. The 5-second window
     * is generous — saveReceivedMessage() and record creation happen in the same
     * call in attemptDecryption(), so the gap is typically <10ms.
     */
    private fun findMatchingMessage(
        record: DecryptedMessageRecord,
        friendName: String?
    ): Message?
    {
        return Persist.messageList.find { msg ->
            !msg.fromMe &&
                    msg.sender?.name == friendName &&
                    kotlin.math.abs(msg.timestamp.timeInMillis - record.timestamp) < 5000L
        }
    }

    /**
     * Decrypts a message's ciphertext on the calling (IO) dispatcher.
     * Returns null if the app is locked or decryption fails — never throws.
     * Plaintext exists only as the return value and is never stored.
     */
    private fun decryptMessage(cipherText: ByteArray, publicKeyBytes: ByteArray): String?
    {
        return try
        {
            Encryption().decrypt(PublicKey(publicKeyBytes), cipherText)
        }
        catch (_: SecurityException)
        {
            Timber.d("App locked — message content redacted")
            null
        }
        catch (e: Exception)
        {
            Timber.w(e, "Decryption failed for message row")
            null
        }
    }

    // ── Row data ──────────────────────────────────────────────────────────────

    /**
     * Intermediate data holder for a single dialog row.
     * Plaintext is held only long enough to build the row view,
     * then this object goes out of scope.
     */
    private data class RowData(
        val timeString: String,
        val spotCount: Int,
        val plaintext: String?  // null = locked or not found
    )

    private fun buildRowData(
        record: DecryptedMessageRecord,
        message: Message?,
        publicKeyBytes: ByteArray?
    ): RowData
    {
        val timeString = message?.getDateStringForDetail() ?: formatTimestamp(record.timestamp)

        val plaintext = when
        {
            message == null -> null

            // Unencrypted messages are stored as raw UTF-8 — no key needed
            !message.isEncrypted -> String(message.cipherText, Charsets.UTF_8)

            // Encrypted path
            publicKeyBytes != null -> decryptMessage(message.cipherText, publicKeyBytes)

            else -> null
        }

        return RowData(timeString, record.spotCount, plaintext)
    }

    // ── View construction ─────────────────────────────────────────────────────

    /**
     * Builds the view for a single message row.
     * Called on the main thread after decryption is complete.
     */
    private fun buildRowView(data: RowData): View
    {
        val ctx = requireContext()
        val vPad = ctx.resources.getDimensionPixelSize(R.dimen.padding_half)
        val hPad = ctx.resources.getDimensionPixelSize(R.dimen.padding_row_horizontal)

        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(hPad, vPad, hPad, vPad)
            background = ContextCompat.getDrawable(ctx, R.drawable.btn_bkgd_light_grey_outline_8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = ctx.resources.getDimensionPixelSize(R.dimen.activity_half_margin)
            }
        }

        // Header: timestamp + spot count
        row.addView(TextView(ctx).apply {
            text = getString(R.string.message_row_header, data.timeString, data.spotCount)
            textSize = 12f
            setTextColor(ContextCompat.getColor(ctx, R.color.coolGrey))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                // padding_quarter = 6dp, matching the original intent
                bottomMargin = ctx.resources.getDimensionPixelSize(R.dimen.padding_quarter)
            }
        })

        // Message content or locked placeholder
        row.addView(TextView(ctx).apply {
            text = data.plaintext ?: getString(R.string.message_content_locked)
            textSize = 15f
            setTextColor(
                ContextCompat.getColor(
                    ctx,
                    if (data.plaintext != null) R.color.white else R.color.coolGrey
                )
            )
        })

        return row
    }

    private fun formatTimestamp(timestampMs: Long): String
    {
        val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())
        return fmt.format(Calendar.getInstance().apply { timeInMillis = timestampMs }.time)
    }

    override fun onDestroyView()
    {
        super.onDestroyView()
        uiScope.cancel()
        _binding = null
    }

    companion object
    {
        private const val ARG_IS_MFSK = "arg_is_mfsk"

        /**
         * @param isMfsk True when opened from [MFSKReceiveRadioBottomSheetFragment] —
         *               reads from [FriendInfoViewModel.mfskDecryptedMessageRecords].
         *               False (default) reads from [FriendInfoViewModel.wsprDecryptedMessageRecords].
         */
        fun newInstance(isMfsk: Boolean = false): ReceivedMessagesDialogFragment =
            ReceivedMessagesDialogFragment().apply {
                arguments = Bundle().apply { putBoolean(ARG_IS_MFSK, isMfsk) }
            }
    }
}
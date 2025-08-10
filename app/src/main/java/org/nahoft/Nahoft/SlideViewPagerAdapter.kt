package org.nahoft.nahoft

import android.app.Activity
import android.content.Context
import android.text.Layout
import android.text.SpannableString
import android.text.method.ScrollingMovementMethod
import android.text.style.AlignmentSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.viewpager.widget.PagerAdapter
import org.nahoft.nahoft.activities.SlideActivity
import org.nahoft.nahoft.databinding.SlideScreenBinding

class SlideViewPagerAdapter(private val context: Context, private val slideList: ArrayList<Slide>) : PagerAdapter() {
    override fun getCount(): Int {
        return slideList.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any
    {
        val binding = SlideScreenBinding.inflate(
            LayoutInflater.from(context),
            container,
            false
        )
        val view = binding.root

        val currentSlide = slideList[position]

        binding.mainImageView.setImageResource(currentSlide.image)
        binding.titleTextView.text = currentSlide.title
        binding.descriptionTextView.text = currentSlide.description
        binding.prevButton.isVisible = position != 0
        binding.nextButton.isVisible = position != slideList.size - 1
        binding.getStartedButton.text = currentSlide.skipButtonText
        binding.readMoreButton.isInvisible = currentSlide.fullDescription.isNullOrEmpty()

        if (currentSlide.showButtonAsLink)
        {
            binding.skipButton.isVisible = true
            binding.getStartedButton.isVisible = false
        }
        else
        {
            binding.skipButton.isVisible = false
            binding.getStartedButton.isVisible = true
        }

        setIndicatorColor(binding, position)

        binding.nextButton.setOnClickListener {
            SlideActivity.viewPager?.currentItem = position + 1
        }

        binding.prevButton.setOnClickListener {
            SlideActivity.viewPager?.currentItem = position - 1
        }

        binding.getStartedButton.setOnClickListener {
            (context as Activity).finish()
        }

        binding.skipButton.setOnClickListener {
            (context as Activity).finish()
        }

        binding.readMoreButton.setOnClickListener {
            currentSlide.fullDescription?.let { desc ->
                showFullDescription(context, currentSlide.title, desc)
            }
        }

        container.addView(view)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    private fun setIndicatorColor(binding: SlideScreenBinding, activeInd: Int)
    {
        val params: LinearLayout.LayoutParams =
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.setMargins(0, 0, 8, 0)
        params.width = 40
        params.height = 40

        for (i in 0 until slideList.size) {
            val ind = ImageView(binding.root.context)
            ind.setImageResource(if (i == activeInd) R.drawable.active_slide_indicator else R.drawable.unactive_slide_indicator)
            ind.layoutParams = params
            binding.indicatorsContainer.addView(ind)
        }
    }

    private fun showFullDescription(context: Context, title: String, body: String)
    {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context, R.style.AppTheme_AlertDialog)
        val titleSpan = SpannableString(title)

        // alert dialog title align center
        titleSpan.setSpan(
            AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
            0,
            titleSpan.length,
            0
        )
        builder.setTitle(titleSpan)

        val inputTextView = TextView(context)
        inputTextView.textAlignment = View.TEXT_ALIGNMENT_CENTER
        inputTextView.setPadding(15)
        inputTextView.text = body
        inputTextView.textSize = 16F
        inputTextView.movementMethod = ScrollingMovementMethod()
        inputTextView.setTextColor(ContextCompat.getColor(context, R.color.black))
        builder.setView(inputTextView)

        builder.setNeutralButton(context.getString(R.string.close)) { dialog, _->
            dialog.cancel()
        }.create().show()
    }
}
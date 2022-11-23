package org.nahoft.nahoft

import android.content.Context

data class Slide constructor(
    val image: Int,
    val title: String,
    val description: String,
    val skipButtonText: String,
    val fullDescription: String? = null
)

const val slideNameIntro = "slideIntro"
const val slideNameSetting = "slideSetting"
const val slideNameAboutAndFriends = "slideAboutAndFriends"

fun getIntroSlides(context: Context): ArrayList<Slide> {
    val arrayList: ArrayList<Slide> = ArrayList()
    arrayList.add(
        Slide(
            image = R.drawable.ic_nahoft_intro_hacker_confused,
            title = context.getString(R.string.what_is_nahoft),
            description = context.getString(R.string.what_is_nahoft_description),
            skipButtonText = context.getString(R.string.skip)
        )
    )
    arrayList.add(
        Slide(
            image = R.drawable.ic_nahoft_intro_secure_image,
            title = context.getString(R.string.send_encrypted_messages),
            description = context.getString(R.string.send_encrypted_messages_description),
            skipButtonText = context.getString(R.string.skip)
        )
    )
    arrayList.add(
        Slide(
            image = R.drawable.ic_nahoft_intro_wifi_offline,
            title = context.getString(R.string.fully_offline),
            description = context.getString(R.string.fully_offline_description),
            skipButtonText = context.getString(R.string.skip)
        )
    )
    arrayList.add(
        Slide(
            image = R.drawable.ic_nahoft_intro_key,
            title = context.getString(R.string.passcode_for_login),
            description = context.getString(R.string.passcode_for_login_description),
            skipButtonText = context.getString(R.string.skip)
        )
    )
    arrayList.add(
        Slide(
            image = R.drawable.ic_nahoft_intro_destruction,
            title = context.getString(R.string.destructionCode),
            description = context.getString(R.string.destructionCodeDescription),
            skipButtonText = context.getString(R.string.get_started)
        )
    )

    return arrayList
}

fun getSettingSlides(context: Context): ArrayList<Slide> {
    val arrayList: ArrayList<Slide> = ArrayList()
    arrayList.add(
        Slide(
            image = R.drawable.ic_nahoft_intro_passcode,
            title = context.getString(R.string.passcode),
            description = context.getString(R.string.passcode_description),
            skipButtonText = context.getString(R.string.close)
        )
    )
    arrayList.add(
        Slide(
            image = R.drawable.ic_nahoft_intro_destruction,
            title = context.getString(R.string.destructionCode),
            description = context.getString(R.string.destruction_code_description),
            skipButtonText = context.getString(R.string.close),
            fullDescription = context.getString(R.string.destruction_code_description)
        )
    )
    arrayList.add(
        Slide(
            image = R.drawable.ic_nahoft_intro_logout,
            title = context.getString(R.string.button_label_logout),
            description = context.getString(R.string.logout_description),
            skipButtonText = context.getString(R.string.close)
        )
    )
    arrayList.add(
        Slide(
            image = R.drawable.ic_nahoft_intro_message_bubble,
            title = context.getString(R.string.use_sms_as_default),
            description = context.getString(R.string.use_sms_as_default_description),
            skipButtonText = context.getString(R.string.close)
        )
    )

    return arrayList
}

fun getAboutAndFriendsSlides(context: Context): ArrayList<Slide> {
    val arrayList: ArrayList<Slide> = ArrayList()
    arrayList.add(
        Slide(
            image = R.drawable.ic_nahoft_intro_about,
            title = context.getString(R.string.button_label_about),
            description = context.getString(R.string.what_is_nahoft_description),
            skipButtonText = context.getString(R.string.close),
            fullDescription = context.getString(R.string.about_user_guide_english)
        )
    )
    arrayList.add(
        Slide(
            image = R.drawable.ic_nahoft_intro_friends,
            title = context.getString(R.string.friends_list),
            description = context.getString(R.string.friends_list_description),
            skipButtonText = context.getString(R.string.close)
        )
    )
    arrayList.add(
        Slide(
            image = R.drawable.ic_nahoft_intro_add_user,
            title = context.getString(R.string.add_new_friend),
            description = context.getString(R.string.add_new_friend_description),
            skipButtonText = context.getString(R.string.close)
        )
    )
    arrayList.add(
        Slide(
            image = R.drawable.ic_nahoft_intro_approve_user,
            title = context.getString(R.string.approve_friend),
            description = context.getString(R.string.approve_friend_description),
            skipButtonText = context.getString(R.string.close),
            fullDescription = context.getString(R.string.approve_friend_full_description)
        )
    )
    arrayList.add(
        Slide(
            image = R.drawable.ic_nahoft_intro_remove_user,
            title = context.getString(R.string.delete_friend_button),
            description = context.getString(R.string.delete_friend_description),
            skipButtonText = context.getString(R.string.close)
        )
    )

    return arrayList
}

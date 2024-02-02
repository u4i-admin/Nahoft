package org.nahoft.nahoft.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import org.nahoft.nahoft.*

class SlideActivity : AppCompatActivity() {
    companion object {
        var viewPager: ViewPager? = null
    }
    var pagerAdapter: SlideViewPagerAdapter? = null
    var isExitWithBack: Boolean = true

    override fun onBackPressed() {
        if (isExitWithBack)
            finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_slide)
        val dataTypeExtra = intent.getStringExtra(Intent.EXTRA_TEXT)
        viewPager = findViewById(R.id.view_pager)
        pagerAdapter = when (dataTypeExtra) {
            slideNameIntro -> {
                isExitWithBack = false
                SlideViewPagerAdapter(this, getIntroSlides(applicationContext))
            }
            slideNameSetting -> {
                SlideViewPagerAdapter(this, getSettingSlides(applicationContext))
            }
            slideNameAboutAndFriends -> {
                SlideViewPagerAdapter(this, getAboutAndFriendsSlides(applicationContext))
            }
//            slideNameContactList -> {
//                SlideViewPagerAdapter(this, getContactSlides(applicationContext))
//            }
            slideNameChat -> {
                SlideViewPagerAdapter(this, getChatSlides(applicationContext))
            }
            slideNameAbout -> {
                SlideViewPagerAdapter(this, getAboutSlides(applicationContext))
            }
            else -> {
                isExitWithBack = false
                SlideViewPagerAdapter(this, getIntroSlides(applicationContext))
            }
        }
        viewPager?.adapter = pagerAdapter
    }
}
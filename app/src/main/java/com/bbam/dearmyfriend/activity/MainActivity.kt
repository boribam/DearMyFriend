package com.bbam.dearmyfriend.activity

import android.location.Location
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bbam.dearmyfriend.R
import com.bbam.dearmyfriend.databinding.ActivityMainBinding
import com.bbam.dearmyfriend.fragment.AnimalFragment
import com.bbam.dearmyfriend.fragment.CalendarFragment
import com.bbam.dearmyfriend.fragment.MapFragment
import com.bbam.dearmyfriend.fragment.MypageFragment
import com.bbam.dearmyfriend.fragment.SocialFragment

class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    var myLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setBottomNavigationView()
    }

    private fun setBottomNavigationView() {

        supportFragmentManager.beginTransaction().add(R.id.fragment_container, CalendarFragment())
            .commit()

        binding.bnv.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.bnv_calendar ->
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, CalendarFragment()).commit()


                R.id.bnv_hospital ->
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, MapFragment()).commit()

                R.id.bnv_social ->
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, SocialFragment()).commit()


                R.id.bnv_animal ->
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, AnimalFragment()).commit()


                R.id.bnv_mypage ->
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, MypageFragment()).commit()
            }

            // 리턴 값을 true --> 선택 변경 UI가 반영됨
            true
        }
    }
}
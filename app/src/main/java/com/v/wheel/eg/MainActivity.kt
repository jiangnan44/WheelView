package com.v.wheel.eg

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.v.wheelview.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.btn_pick_weekday).setOnClickListener(::showWeekDialog)
        findViewById<View>(R.id.btn_pick_date).setOnClickListener(::showDateDialog)
    }


    private var selectWeekday = "Wednesday"
    private fun showWeekDialog(view: View?) {

        PickWeekdayDialog()
            .withInitDay(selectWeekday)
            .withWeekdayChooseListener { chosenDay ->
                selectWeekday = chosenDay
                Toast.makeText(this, "picked:$chosenDay", Toast.LENGTH_SHORT).show()
            }
            .show(supportFragmentManager)
    }


    private var selectDate = "2023-5-12"
    private fun showDateDialog(view: View?) {

        PickDateDialog()
            .withYearRang(start = 1990, end = 2030)
            .withInitDate(selectDate)
            .withDateChooseListener { chosenDay ->
                selectDate = chosenDay
                Toast.makeText(this, "picked:$chosenDay", Toast.LENGTH_SHORT).show()
            }
            .show(supportFragmentManager)
    }


}
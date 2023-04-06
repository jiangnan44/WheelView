package com.v.wheel.eg

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.v.wheel.lib.WheelView
import com.v.wheelview.R

/**
 * Author:v
 * Time:2020/11/25
 */
class PickWeekdayDialog : DialogFragment() {

    private val TAG = "PickWeekdayDialog"

    private var selectMonth: String = ""
    private var onDateChoose: ((action: String) -> Unit)? = null


    override fun getTheme() = R.style.Theme_WheelView_BottomDialog


    override fun onStart() {
        super.onStart()
        setWindow()
    }

    private fun setWindow() {
        dialog?.apply {
            window?.run {
                attributes = attributes.apply {
                    gravity = Gravity.BOTTOM
                    width = WindowManager.LayoutParams.MATCH_PARENT
                    height = WindowManager.LayoutParams.WRAP_CONTENT
                }
            }
            setCancelable(true)
            setCanceledOnTouchOutside(true)
        }
    }

    fun show(manager: FragmentManager) {
        activity?.let {
            if (it.isFinishing || it.isDestroyed) return
        }

        val f = manager.findFragmentByTag(TAG)
        if (f?.isAdded == true) {
            manager.beginTransaction().show(f)
        } else {
            super.show(manager, TAG)
        }
    }


    fun withWeekdayChooseListener(listener: ((string: String) -> Unit)): PickWeekdayDialog {
        onDateChoose = listener
        return this
    }


    fun withInitDay(selectDay: String): PickWeekdayDialog {
        selectMonth = selectDay
        return this
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.dialog_pick_weekday, container, false)
        root.findViewById<View>(R.id.tv_cancel).setOnClickListener {
            dismissAllowingStateLoss()
        }

        root.findViewById<View>(R.id.tv_confirm).setOnClickListener {
            onDateChoose?.invoke(selectMonth)
            dismissAllowingStateLoss()
        }
        initWheelView(root)
        return root
    }


    private fun initWheelView(root: View) {
        root.findViewById<WheelView>(R.id.wv_weekday).apply {
            val list = initMonthList()
            var i = list.indexOf(selectMonth)
            if (i < 0) {
                i = list.size / 2
            }

            setupStringItems(list)
            setCurrentSelect(i)
            selectMonth = list[i]
            setOnItemSelectedListener { item ->
                Log.d(TAG, "onItemSelected index=${item.index()} content=${item.text()}")
                selectMonth = item.text()
            }
        }
    }


    private fun initMonthList() = listOf(
        "Monday",
        "Tuesday",
        "Wednesday",
        "Thursday",
        "Friday",
        "Saturday",
        "Sunday",
    )


}

package technology.nine.cred.activity

import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.android.synthetic.main.activity_main.*
import technology.nine.cred.R
import technology.nine.cred.`interface`.StateChangeListener
import technology.nine.cred.fragments.FirstFragment
import technology.nine.cred.fragments.SecondFragment
import technology.nine.cred.utills.BottomSheetBehavior


class MainActivity : AppCompatActivity(), StateChangeListener {

    private var firstBottomSheetBehavior: BottomSheetBehavior<*>? = null
    private var secondBottomSheetBehaviour: BottomSheetBehavior<*>? = null
    var height: Int = 0
    var width: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        seek_bar.points = 10

        supportFragmentManager.beginTransaction()
            .add(R.id.first_fragment_container, FirstFragment())
            .commit()

        supportFragmentManager.beginTransaction()
            .add(R.id.second_fragment_container, SecondFragment())
            .commit()

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        height = displayMetrics.heightPixels
        width = displayMetrics.widthPixels


        val firstBottomSheet: FrameLayout = findViewById(R.id.first_bottom_sheet_holder)
        val secondBottomSheet: FrameLayout = findViewById(R.id.second_bottom_sheet_holder)
        firstBottomSheetBehavior =
            BottomSheetBehavior.from(firstBottomSheet) as BottomSheetBehavior<*>
        secondBottomSheetBehaviour =
            BottomSheetBehavior.from(secondBottomSheet) as BottomSheetBehavior<*>

        firstBottomSheetBehavior?.isDraggable = false
        secondBottomSheetBehaviour?.isDraggable = false



        firstBottomSheet.layoutParams.height = height - 550
        secondBottomSheet.layoutParams.height = height - 800
        bt_proceed.setOnClickListener {
            if (firstBottomSheetBehavior?.state == BottomSheetBehavior.STATE_COLLAPSED) {
                firstBottomSheetBehavior!!.state = BottomSheetBehavior.STATE_EXPANDED;
            } else if (firstBottomSheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED) {
                secondBottomSheetBehaviour!!.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        firstBottomSheetBehavior!!.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    bt_proceed.text = getString(R.string.select_your_bank_account)
                    second_bottom_sheet_holder.visibility = View.VISIBLE

                }
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    second_bottom_sheet_holder.visibility = View.GONE
                    bt_proceed.text = getString(R.string.proceed_to_emi_selection)

                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (slideOffset > 0.6) {

                    collapsed_layout.visibility = View.VISIBLE
                    expanded_layout.visibility = View.GONE
                } else {
                    collapsed_layout.visibility = View.GONE
                    expanded_layout.visibility = View.VISIBLE
                }
            }
        })


        secondBottomSheetBehaviour!!.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_DRAGGING || newState == BottomSheetBehavior.STATE_EXPANDED) {
                    bt_proceed.text = getString(R.string.tap_for_click_kyc)

                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {

                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {

                if (slideOffset > 0.6) {
                    setLocalBoardCast("STATE_EXPANDED")
                } else {
                    setLocalBoardCast("STATE_COLLAPSED")

                }
            }
        })


        collapsed_layout.setOnClickListener {
            secondBottomSheetBehaviour!!.state = BottomSheetBehavior.STATE_COLLAPSED
            firstBottomSheetBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED

        }


    }

    private fun setLocalBoardCast(state: String) {
        val localBroadcastManager = LocalBroadcastManager.getInstance(this@MainActivity)
        val broadcastIntent = Intent(state)
        localBroadcastManager.sendBroadcast(broadcastIntent)
    }

    override fun onBackPressed() {
        if (secondBottomSheetBehaviour?.state == BottomSheetBehavior.STATE_EXPANDED) {
            secondBottomSheetBehaviour!!.state = BottomSheetBehavior.STATE_COLLAPSED
        } else if (firstBottomSheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED
            && secondBottomSheetBehaviour?.state == BottomSheetBehavior.STATE_COLLAPSED
        ) {
            firstBottomSheetBehavior!!.state = BottomSheetBehavior.STATE_COLLAPSED

        } else {
            super.onBackPressed()
        }

    }

    override fun onStateChange() {
        secondBottomSheetBehaviour!!.state = BottomSheetBehavior.STATE_COLLAPSED
    }


}
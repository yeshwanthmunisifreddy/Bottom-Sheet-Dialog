package technology.nine.cred.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_first.*
import technology.nine.cred.R
import technology.nine.cred.`interface`.StateChangeListener
import technology.nine.cred.adapter.EmiCardAdapter
import technology.nine.cred.model.EmiDetails


class FirstFragment : Fragment() {

    private lateinit var adapter: EmiCardAdapter
    private var emiList = mutableListOf<EmiDetails>()

    private var mContext: Context? = null
    private var mBroadcastReceiver: BroadcastReceiver? = null

    private var listener: StateChangeListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context;
        listener = context as StateChangeListener
        mBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                if ("STATE_COLLAPSED" == action) {
                    collapsed_layout.visibility = View.GONE
                    expanded_layout.visibility = View.VISIBLE
                } else if ("STATE_EXPANDED" == action) {
                    collapsed_layout.visibility = View.VISIBLE
                    expanded_layout.visibility = View.GONE
                }
            }
        }
        val localBroadcastManager = LocalBroadcastManager.getInstance(
            mContext!!
        )
        val intentFilter = IntentFilter()
        intentFilter.addAction("STATE_EXPANDED")
        intentFilter.addAction("STATE_COLLAPSED")
        localBroadcastManager.registerReceiver(
            mBroadcastReceiver as BroadcastReceiver,
            intentFilter
        )


    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.let {
            emiList = getEmiList(it)
            adapter = EmiCardAdapter(it, emiList)
            recycler_view.layoutManager = LinearLayoutManager(it, RecyclerView.HORIZONTAL, false)
            recycler_view.adapter = adapter
            adapter.notifyDataSetChanged()
        }

        collapsed_layout.setOnClickListener {
            listener?.onStateChange()
        }
    }


    private fun getEmiList(context: Context): MutableList<EmiDetails> {
        var emi = mutableListOf<EmiDetails>()
        emi.add(
            EmiDetails(
                4270.0, 12,
                true, ContextCompat.getColor(context, R.color.brown_1),
                ContextCompat.getColor(context, R.color.brown_2)
            )
        )
        emi.add(
            EmiDetails(
                5580.0, 9,
                false, ContextCompat.getColor(context, R.color.indigo_1),
                ContextCompat.getColor(context, R.color.indigo_2)
            )
        )
        emi.add(
            EmiDetails(
                8250.0, 3,
                false, ContextCompat.getColor(context, R.color.blue_7),
                ContextCompat.getColor(context, R.color.blue_8)
            )
        )
        emi.add(
            EmiDetails(
                24070.0, 1,
                false, ContextCompat.getColor(context, R.color.blue_grey_1),
                ContextCompat.getColor(context, R.color.blue_grey_2)
            )
        )

        return emi

    }

    override fun onDetach() {
        super.onDetach()
        val localBroadcastManager = LocalBroadcastManager.getInstance(mContext!!)
        localBroadcastManager.unregisterReceiver(mBroadcastReceiver!!)
        mContext = null
    }


}
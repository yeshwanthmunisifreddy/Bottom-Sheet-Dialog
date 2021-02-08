package technology.nine.cred.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import technology.nine.cred.R
import technology.nine.cred.model.BankDetails


class BankCardAdapter(var context: Context, private val bankList: MutableList<BankDetails>) :
    RecyclerView.Adapter<BankCardAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.emi_item_list, parent, false)
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bankDetails = bankList[position]

        holder.tx_amount.text = bankDetails.bankName
        holder.tx_months.text = bankDetails.bankAccount


    }

    override fun getItemCount(): Int {
        return bankList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var checked: ImageView = itemView.findViewById(R.id.checked)
        var unchecked: ImageView = itemView.findViewById(R.id.unchecked)
        var tx_amount: TextView = itemView.findViewById(R.id.tx_amount)
        var tx_months: TextView = itemView.findViewById(R.id.tx_months)
        var contain_layout: LinearLayout = itemView.findViewById(R.id.contain_layout)
        var main_layout: RelativeLayout = itemView.findViewById(R.id.main_layout)
    }


}

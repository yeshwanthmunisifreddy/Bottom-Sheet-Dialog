package technology.nine.cred.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import technology.nine.cred.R
import technology.nine.cred.model.EmiDetails


class EmiCardAdapter(var context: Context, private val emiList: MutableList<EmiDetails>) :
    RecyclerView.Adapter<EmiCardAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.emi_item_list, parent, false)
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val emiDetails = emiList[position]


        var buttonDrawable: Drawable? = holder.contain_layout.background
        buttonDrawable = DrawableCompat.wrap(buttonDrawable!!)
        DrawableCompat.setTint(buttonDrawable, emiDetails.bg_color)
        holder.contain_layout.background = buttonDrawable

        var checkDrawable = holder.checked.background
        checkDrawable = DrawableCompat.wrap(checkDrawable!!)
        DrawableCompat.setTint(checkDrawable, emiDetails.checked_color)
        holder.checked.background = checkDrawable


        if (emiDetails.isChecked) {
            holder.checked.visibility = View.VISIBLE
            holder.unchecked.visibility = View.GONE
        } else {
            holder.checked.visibility = View.GONE
            holder.unchecked.visibility = View.VISIBLE

        }
        holder.tx_amount.text = "\u20b9${emiDetails.amount}"
        if (emiDetails.amount > 1)
            holder.tx_months.text = "for ${emiDetails.months} months"
        else
            holder.tx_months.text = "for ${emiDetails.months} month"


    }

    override fun getItemCount(): Int {
        return emiList.size
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

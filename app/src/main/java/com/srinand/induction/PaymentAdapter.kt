package com.srinand.induction

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PaymentAdapter(
    private val paymentAppList: List<PaymentApp>,
    private val listener: OnPaymentClickListener
) : RecyclerView.Adapter<PaymentAdapter.PaymentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.payment_item, parent, false)
        return PaymentViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
        val paymentApp = paymentAppList[position]
        holder.paymentIcon.setImageResource(paymentApp.logoResourceId)
        holder.paymentAppName.text = paymentApp.name

        holder.itemView.setOnClickListener {
            listener.onPaymentClick(paymentApp)
        }
    }

    override fun getItemCount() = paymentAppList.size

    class PaymentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val paymentIcon: ImageView = itemView.findViewById(R.id.paymentIcon)
        val paymentAppName: TextView = itemView.findViewById(R.id.paymentAppName)
    }

    interface OnPaymentClickListener {
        fun onPaymentClick(paymentApp: PaymentApp)
    }
}

/*
 * Copyright (C) 2012-2016 The Android Money Manager Ex Project Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.money.manager.ex.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CursorAdapter;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.money.manager.ex.R;
import com.money.manager.ex.core.TransactionTypes;
import com.money.manager.ex.core.UIHelper;
import com.money.manager.ex.currency.CurrencyService;
import com.money.manager.ex.database.QueryBillDeposits;
import com.money.manager.ex.servicelayer.RecurringTransactionService;
import com.money.manager.ex.utils.MyDateTimeUtils;

import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.Locale;

import info.javaperformance.money.MoneyFactory;

public class RecurringTransactionAdapter
        extends CursorAdapter {

    private LayoutInflater inflater;
    private HashMap<String, Integer> mHeadersIndexAccountDate;

    @SuppressWarnings("deprecation")
    public RecurringTransactionAdapter(Context context, Cursor c) {
        super(context, c);
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mHeadersIndexAccountDate = new HashMap<>();
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // take a pointer of object UI
        ImageView imgClock = (ImageView) view.findViewById(R.id.imageViewClock);
        TextView txtDate = (TextView) view.findViewById(R.id.textViewDate);
        TextView txtRepeat = (TextView) view.findViewById(R.id.textViewRepeat);
        TextView txtNextDueDate = (TextView) view.findViewById(R.id.textViewNextDueDate);
        TextView txtAmount = (TextView) view.findViewById(R.id.textViewAmount);
        TextView txtAccountName = (TextView) view.findViewById(R.id.textViewAccountName);
        TextView txtPayee = (TextView) view.findViewById(R.id.textViewPayee);
        LinearLayout linearPayee = (LinearLayout) view.findViewById(R.id.linearLayoutPayee);
        LinearLayout linearToAccount = (LinearLayout) view.findViewById(R.id.linearLayoutToAccount);
        TextView txtToAccountName = (TextView) view.findViewById(R.id.textViewToAccountName);
        TextView txtCategorySub = (TextView) view.findViewById(R.id.textViewCategorySub);
        TextView txtNotes = (TextView) view.findViewById(R.id.textViewNotes);
        ImageView imgFollowUp = (ImageView) view.findViewById(R.id.imageViewFollowUp);
        // compose group
        if (!mHeadersIndexAccountDate.containsKey(cursor.getString(cursor.getColumnIndex(QueryBillDeposits.USERNEXTOCCURRENCEDATE)))) {
            mHeadersIndexAccountDate.put(cursor.getString(cursor.getColumnIndex(QueryBillDeposits.USERNEXTOCCURRENCEDATE)), cursor.getPosition());
        }
        // account name
        txtAccountName.setText(cursor.getString(cursor.getColumnIndex(QueryBillDeposits.ACCOUNTNAME)));

        // Payment Date
        String paymentDate = cursor.getString(cursor.getColumnIndex(QueryBillDeposits.NEXTOCCURRENCEDATE));
        DateTime dateTime = MyDateTimeUtils.from(paymentDate);
        Locale locale = context.getResources().getConfiguration().locale;
        String displayValue = dateTime.toString("EEEE dd MMMM yyyy", locale);
        txtDate.setText(displayValue);

        // take daysleft
        int daysLeft = cursor.getInt(cursor.getColumnIndex(QueryBillDeposits.DAYSLEFT));
        if (daysLeft == 0) {
            txtNextDueDate.setText(R.string.due_today);
        } else {
            txtNextDueDate.setText(Integer.toString(Math.abs(daysLeft)) + " " + context.getString(daysLeft > 0
                    ? R.string.days_remaining : R.string.days_overdue));
            imgClock.setVisibility(daysLeft < 0 ? View.VISIBLE : View.INVISIBLE);
        }
        // show group
        txtDate.setVisibility(mHeadersIndexAccountDate.containsValue(cursor.getPosition()) ? View.VISIBLE : View.GONE);
        txtNextDueDate.setVisibility(txtDate.getVisibility());

        // show follow up icon
        if ("F".equalsIgnoreCase(cursor.getString(cursor.getColumnIndex(QueryBillDeposits.STATUS)))) {
            imgFollowUp.setVisibility(View.VISIBLE);
        } else {
            imgFollowUp.setVisibility(View.GONE);
        }

        int recurrence = cursor.getInt(cursor.getColumnIndex(QueryBillDeposits.REPEATS));
        String recurrenceString = new RecurringTransactionService(context).getRecurrenceLocalizedName(recurrence);
        txtRepeat.setText(recurrenceString);

        // take transaction amount
        double amount = cursor.getDouble(cursor.getColumnIndex(QueryBillDeposits.AMOUNT));
        // manage transfer and change amount sign
        String transCode = cursor.getString(cursor.getColumnIndex(QueryBillDeposits.TRANSCODE));
        boolean isTransfer = TransactionTypes.valueOf(transCode).equals(TransactionTypes.Transfer);

        if ((cursor.getString(cursor.getColumnIndex(QueryBillDeposits.TRANSCODE)) != null)
                && isTransfer) {
            if (cursor.getInt(cursor.getColumnIndex(QueryBillDeposits.ACCOUNTID)) != cursor.getInt(cursor.getColumnIndex(QueryBillDeposits.TOACCOUNTID))) {
                amount = -(amount); // -total
            } else if (cursor.getInt(cursor.getColumnIndex(QueryBillDeposits.TOACCOUNTID)) == cursor.getInt(cursor
                    .getColumnIndex(QueryBillDeposits.TOACCOUNTID))) {
                amount = cursor.getDouble(cursor.getColumnIndex(QueryBillDeposits.TOTRANSAMOUNT)); // to account = account
            }
        }
        CurrencyService currencyService = new CurrencyService(mContext);
        txtAmount.setText(currencyService.getCurrencyFormatted(
                cursor.getInt(cursor.getColumnIndex(QueryBillDeposits.CURRENCYID)),
                MoneyFactory.fromDouble(amount)));
        // check amount sign
        txtAmount.setTextColor(ContextCompat.getColor(context, (amount > 0
                ? UIHelper.getColor(context, R.attr.holo_green_color_theme)
                : UIHelper.getColor(context, R.attr.holo_red_color_theme))));
        // compose payee description
        String payee = cursor.getString(cursor.getColumnIndex(QueryBillDeposits.PAYEENAME));
        // write payee
        if ((!TextUtils.isEmpty(payee))) {
            txtPayee.setText(payee);
            txtPayee.setVisibility(View.VISIBLE);
            linearPayee.setVisibility(View.VISIBLE);
        } else {
            txtPayee.setVisibility(View.GONE);
            linearPayee.setVisibility(View.GONE);
        }
        // write ToAccountName
        if ((!TextUtils.isEmpty(cursor.getString(cursor.getColumnIndex(QueryBillDeposits.TOACCOUNTNAME))))) {
            txtToAccountName.setText(cursor.getString(cursor.getColumnIndex(QueryBillDeposits.TOACCOUNTNAME)));
            linearToAccount.setVisibility(View.VISIBLE);
        } else {
            linearToAccount.setVisibility(View.GONE);
        }
        // compose category description
        String categorySub = cursor.getString(cursor.getColumnIndex(QueryBillDeposits.CATEGNAME));
        // add if not null subcategory
        if (!(TextUtils.isEmpty(cursor.getString(cursor.getColumnIndex(QueryBillDeposits.SUBCATEGNAME))))) {
            categorySub += " : <i>" + cursor.getString(cursor.getColumnIndex(QueryBillDeposits.SUBCATEGNAME)) + "</i>";
        }
        // write category / subcategory format html
        if (TextUtils.isEmpty(categorySub) == false) {
            txtCategorySub.setText(Html.fromHtml(categorySub));
        } else {
            txtCategorySub.setText("");
        }
        // notes
        String notes = cursor.getString(cursor.getColumnIndex(QueryBillDeposits.NOTES));
        if (!TextUtils.isEmpty(notes)) {
            txtNotes.setText(Html.fromHtml("<small>" + notes + "</small>"));
            txtNotes.setVisibility(View.VISIBLE);
        } else {
            txtNotes.setVisibility(View.GONE);
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return inflater.inflate(R.layout.item_bill_deposits, parent, false);
    }
}

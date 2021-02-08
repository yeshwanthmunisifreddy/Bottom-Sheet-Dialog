package technology.nine.cred.utills.customfonts.TextView;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

public class KumbhSansRegularTextView extends AppCompatTextView {

    public KumbhSansRegularTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public KumbhSansRegularTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public KumbhSansRegularTextView(Context context) {
        super(context);
        init();
    }

    private void init() {
        if (!isInEditMode()) {
            Typeface tf = Typeface.createFromAsset(getContext().getAssets(), "KumbhSans-Regular.ttf");
            setTypeface(tf);
        }
    }
}

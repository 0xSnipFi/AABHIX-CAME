package com.aabhix.camera;

import android.app.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.function.*;

/** Full-page settings. One registered handler per row, with grouped cards and search. */
public final class SettingsSheet extends Dialog {
    private static final class Entry {String group,title,detail;Runnable action;BooleanSupplier checked;Consumer<Boolean> change;Entry(String g,String t,String d){group=g;title=t;detail=d;}}
    private final Activity activity;private final String pageTitle;private final ArrayList<Entry> entries=new ArrayList<>();private LinearLayout rows;private EditText search;
    public SettingsSheet(Activity activity){this(activity,"Camera settings");} public SettingsSheet(Activity activity,String title){super(activity,android.R.style.Theme_Material_NoActionBar);this.activity=activity;this.pageTitle=title;}
    public void action(String group,String title,String detail,Runnable action){Entry e=new Entry(group,title,detail);e.action=action;entries.add(e);}
    public void toggle(String group,String title,String detail,BooleanSupplier state,Consumer<Boolean> change){Entry e=new Entry(group,title,detail);e.checked=state;e.change=change;entries.add(e);}
    @Override public void show(){
        requestWindowFeature(Window.FEATURE_NO_TITLE);LinearLayout root=new LinearLayout(activity);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.BLACK);
        LinearLayout bar=new LinearLayout(activity);bar.setGravity(Gravity.CENTER_VERTICAL);Button back=new Button(activity);back.setText("‹");back.setTextSize(30);back.setTextColor(Color.WHITE);back.setBackgroundColor(Color.TRANSPARENT);back.setContentDescription("Close "+pageTitle);back.setOnClickListener(v->dismiss());bar.addView(back,new LinearLayout.LayoutParams(dp(56),dp(60)));TextView heading=label(pageTitle,24,Color.WHITE);heading.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));bar.addView(heading,new LinearLayout.LayoutParams(0,-2,1));root.addView(bar);
        search=new EditText(activity);search.setSingleLine();search.setHint("Search "+pageTitle.toLowerCase(Locale.ROOT));search.setContentDescription("Search "+pageTitle);search.setTextSize(16);search.setTextColor(Color.WHITE);search.setHintTextColor(0xffa0a0a0);search.setPadding(dp(16),0,dp(16),0);search.setBackground(round(0xff202124));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(48));sp.setMargins(dp(20),dp(8),dp(20),dp(8));root.addView(search,sp);
        ScrollView scroll=new ScrollView(activity);scroll.setFillViewport(true);rows=new LinearLayout(activity);rows.setOrientation(LinearLayout.VERTICAL);rows.setPadding(dp(20),0,dp(20),dp(28));scroll.addView(rows);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        root.setOnApplyWindowInsetsListener((view,insets)->{if(android.os.Build.VERSION.SDK_INT>=30){android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());view.setPadding(i.left,i.top,i.right,i.bottom);}else view.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){render();}public void afterTextChanged(Editable e){}});setContentView(root);super.show();Window window=getWindow();if(window!=null){window.setLayout(-1,-1);window.setStatusBarColor(Color.BLACK);window.setNavigationBarColor(Color.BLACK);window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN|WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);}render();
    }
    private void render(){rows.removeAllViews();String query=search.getText().toString().trim().toLowerCase(Locale.ROOT);LinkedHashMap<String,List<Entry>> groups=new LinkedHashMap<>();for(Entry e:entries)if((e.group+" "+e.title+" "+e.detail).toLowerCase(Locale.ROOT).contains(query))groups.computeIfAbsent(e.group,key->new ArrayList<>()).add(e);
        for(Map.Entry<String,List<Entry>> group:groups.entrySet()){TextView section=label(group.getKey(),13,0xffaaaaaa);section.setPadding(dp(12),dp(24),0,dp(10));rows.addView(section);LinearLayout card=new LinearLayout(activity);card.setOrientation(LinearLayout.VERTICAL);card.setBackground(round(0xff1c1c1e));rows.addView(card);
            for(Entry e:group.getValue()){if(card.getChildCount()>0){View divider=new View(activity);divider.setBackgroundColor(0xff353537);LinearLayout.LayoutParams line=new LinearLayout.LayoutParams(-1,dp(1));line.setMargins(dp(16),0,dp(16),0);card.addView(divider,line);}LinearLayout row=new LinearLayout(activity);row.setGravity(Gravity.CENTER_VERTICAL);row.setMinimumHeight(dp(64));row.setPadding(dp(16),dp(14),dp(16),dp(14));LinearLayout text=new LinearLayout(activity);text.setOrientation(LinearLayout.VERTICAL);text.addView(label(e.title,16,Color.WHITE));if(!e.detail.isEmpty()){TextView detail=label(e.detail,12,0xffaaaaaa);detail.setPadding(0,dp(5),dp(10),0);text.addView(detail);}row.addView(text,new LinearLayout.LayoutParams(0,-2,1));
                if(e.checked!=null){Switch toggle=new Switch(activity);toggle.setContentDescription(e.title);toggle.setChecked(e.checked.getAsBoolean());toggle.setButtonTintList(null);toggle.setOnCheckedChangeListener((v,on)->e.change.accept(on));row.addView(toggle);row.setOnClickListener(v->toggle.setChecked(!toggle.isChecked()));}
                else if(e.action!=null){row.addView(label("›",26,0xffaaaaaa));row.setOnClickListener(v->{dismiss();e.action.run();});row.setFocusable(true);row.setContentDescription(e.title+". "+e.detail);}card.addView(row);}
        }if(groups.isEmpty())rows.addView(label("No matching settings",15,Color.LTGRAY));
    }
    private GradientDrawable round(int color){GradientDrawable drawable=new GradientDrawable();drawable.setColor(color);drawable.setCornerRadius(dp(16));return drawable;}
    private TextView label(String value,int size,int color){TextView t=new TextView(activity);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));return t;}
    private int dp(float value){return Math.round(value*activity.getResources().getDisplayMetrics().density);}
}

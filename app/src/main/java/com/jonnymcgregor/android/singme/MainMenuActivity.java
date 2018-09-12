package com.jonnymcgregor.android.singme;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class MainMenuActivity extends AppCompatActivity {
    final Context context = this;
    public Button mainButton;
    public Button helpButton;
    public ImageView helpMenuImage;
    public TextView helpMenuTitle;
    public TextView paragraph1;
    public TextView paragraph2;
    public TextView paragraph3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main_menu);

        mainButton = (Button) findViewById(R.id.mainButton);
        helpButton = (Button) findViewById(R.id.btnHelp);
        helpMenuImage = (ImageView)findViewById(R.id.helpBackground);
        helpMenuTitle = (TextView)findViewById((R.id.helpTitle));
        paragraph1 = (TextView)findViewById((R.id.paragraph1));
        paragraph2 = (TextView)findViewById((R.id.paragraph2));
        paragraph3 = (TextView)findViewById((R.id.paragraph3));

    }

    public void helpClicked(View view)
    {
       if(helpMenuImage.getVisibility() == View.INVISIBLE) {
            helpMenuImage.setVisibility(View.VISIBLE);
            helpMenuTitle.setVisibility(View.VISIBLE);
            paragraph1.setVisibility(View.VISIBLE);
            paragraph2.setVisibility(View.VISIBLE);
            paragraph3.setVisibility(View.VISIBLE);

            mainButton.setActivated(false);
            mainButton.setVisibility(View.INVISIBLE);
       }

       else
       {
           helpMenuImage.setVisibility(View.INVISIBLE);
           helpMenuTitle.setVisibility(View.INVISIBLE);
           paragraph1.setVisibility(View.INVISIBLE);
           paragraph2.setVisibility(View.INVISIBLE);
           paragraph3.setVisibility(View.INVISIBLE);

           mainButton.setActivated(true);
           mainButton.setVisibility(View.VISIBLE);

       }
    }

    public void OnClick(View view)
    {
        changeActivity();
    }

    private void changeActivity() {
        Intent intent = new Intent(this, GameActivity.class);
        startActivity(intent);
    }
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }
}

package com.jonnymcgregor.android.singme;

import android.content.Intent;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.sql.SQLOutput;
import java.util.LinkedList;
import java.util.Random;


public class GameActivity extends AppCompatActivity {

    //Audio Params Setup
    private static final int RECORDER_SAMPLERATE = 44100;

    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;

    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_FLOAT;

    private static final String TAG = null;
    private final Object GameActivity = this;

    private int RECORDER_BUFFERSIZE = 2048;

    private AudioRecord recorder = null;

    private MediaPlayer mediaPlayer = null;

    private boolean isRecording = false;

    //Thread Instantiation
    private Thread recordingThread = null;

    private Thread visualThread = null;

    private Thread gameLoopThread = null;

    //FFT Object setup
    public float numberOfBins = RECORDER_BUFFERSIZE / 2;

    public float frequencyResolution = RECORDER_SAMPLERATE / RECORDER_BUFFERSIZE;

    public int lowestBinForVocals = 140 / ((int) frequencyResolution);

    public FFT fft;

    public double[] emptyArray = new double[RECORDER_BUFFERSIZE];

    public double[] realArray = new double[RECORDER_BUFFERSIZE];

    public float fundamentalFrequency;

    private TextView timerText;

    public int startTime = 30000;

    private long timeRemaining;

    private CountDownTimerPausable timer;

    private ToggleButton[] bubble = new ToggleButton[10];

    private Button pauseButton;

    private int[] audioId = new int[10];

    private float buttonFreq[] = new float[10];

    private int levelNo = 1;

    private int activeButton;

    private int numberOfButtons = 3;

    private float noteArray[] = new float[13];

    private int audioArray[] = new int[13];

    public int avgCounter = 0;

    public double averageFundamental = 0;

    public boolean removeActiveButton = false;

    private void setAudioIds() {
        audioArray[0] = R.raw.c4;
        audioArray[1] = R.raw.db4;
        audioArray[2] = R.raw.d4;
        audioArray[3] = R.raw.eb4;
        audioArray[4] = R.raw.e4;
        audioArray[5] = R.raw.f4;
        audioArray[6] = R.raw.gb4;
        audioArray[7] = R.raw.g4;
        audioArray[8] = R.raw.ab4;
        audioArray[9] = R.raw.a4;
        audioArray[10] = R.raw.bb4;
        audioArray[11] = R.raw.b4;
        audioArray[12] = R.raw.c5;
    }

    private void setNotes() {
        noteArray[0] = 261.63f;
        noteArray[1] = 277.18f;
        noteArray[2] = 293.66f;
        noteArray[3] = 311.13f;
        noteArray[4] = 329.63f;
        noteArray[5] = 349.23f;
        noteArray[6] = 369.99f;
        noteArray[7] = 392.00f;
        noteArray[8] = 415.30f;
        noteArray[9] = 440.00f;
        noteArray[10] = 466.16f;
        noteArray[11] = 493.88f;
        noteArray[12] = 523.25f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_game);
        setButtonHandlers();
        System.out.println("BUFFERSIZE: " + RECORDER_BUFFERSIZE);
        setNotes();
        setAudioIds();
        timerText = (TextView) findViewById(R.id.timerText);
        timerText.setTextColor(Color.WHITE);
        fft = new FFT(2048);
        StartLevel();
    }

    private void VisualCanvasCreate(){
        visualThread = new Thread (new Runnable(){
            public void run() {
                initialiseButtons();
                //int test = 0;
                while(true){
                    if (removeActiveButton == true) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            bubble[activeButton].setChecked(false);
                            bubble[activeButton].setActivated(false);
                            bubble[activeButton].setVisibility(View.INVISIBLE);
                            numberOfButtons--;
                            System.out.println("Removing active button...");

                        }
                    });
                    }
                }
            }
        }, "Visual Thread");
        visualThread.start();
    }
    private void StartLevel() {
        setLevelParameters();
        initialiseButtons();
        timeRemaining = startTime;
        startTimer();
        MainLoop();
    }
    private void MainLoop() {
        gameLoopThread = new Thread(new Runnable(){
            public void run(){
                System.out.println("GameLoop has Begun");
                while(true) {
                    if (numberOfButtons == 0) {
                        //WINNER!!!
                        gameLoopThread = null;
                    }
                    if (timeRemaining == 0) {
                        //LOSER!!!
                        System.out.println("You Lose!");
                         mediaPlayer.stop();
                         stopRecording();
                        Intent intent = new Intent(getApplicationContext(), MainMenuActivity.class);
                        startActivity(intent);
                    }
                }
            }
        },"Game Loop Thread");
        gameLoopThread.start();
    }
    private void initialiseButtons() {
        for (int i = 0; i < numberOfButtons; i++) {
            bubble[i].setVisibility(View.VISIBLE);
        }
        pauseButton.setOnClickListener(pauseClick);
    }

    private void setButtonHandlers() {
        bubble[0] = (ToggleButton) findViewById(R.id.bubble1);
        bubble[1] = (ToggleButton) findViewById(R.id.bubble2);
        bubble[2] = (ToggleButton) findViewById(R.id.bubble3);
        bubble[3] = (ToggleButton) findViewById(R.id.bubble4);
        bubble[4] = (ToggleButton) findViewById(R.id.bubble5);
        bubble[5] = (ToggleButton) findViewById(R.id.bubble6);
        bubble[6] = (ToggleButton) findViewById(R.id.bubble7);
        bubble[7] = (ToggleButton) findViewById(R.id.bubble8);
        bubble[8] = (ToggleButton) findViewById(R.id.bubble9);
        bubble[9] = (ToggleButton) findViewById(R.id.bubble10);
        pauseButton = (Button) findViewById(R.id.btnPause);
    }
    private void setLevelParameters() {
        numberOfButtons = 3;
        for (int i = 0; i < numberOfButtons; i++) {
            int random = new Random().nextInt(6);
            buttonFreq[i] = noteArray[random];
            audioId[i] = audioArray[random];
            //Check buttonFreq and audioId are matching
            System.out.println(buttonFreq[i] + " = " + audioId[i]);
        }
        startTime = 15000;
    }

    private void checkBubbles() {
        if (bubble[activeButton].isChecked() && averageFundamental < (buttonFreq[activeButton] + frequencyResolution)
                && averageFundamental > (buttonFreq[activeButton] - frequencyResolution)) {

            System.out.println("Avg Fundamental: " + averageFundamental + " Active Frequency: " + buttonFreq[activeButton]);

            stopRecording();
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "error:" + e.getMessage());
            }
            removeActiveButton();
            averageFundamental = 0;
           }
    }


    private void startTimer() {
        timer = new CountDownTimerPausable(timeRemaining, 1000) {

            public void onTick(long millisUntilFinished) {
                timeRemaining = millisUntilFinished;
                timerText.setText("" + timeRemaining / 1000);
            }

            public void onFinish() {
                timerText.setText("done!");
            }

        }.start();
    }

    public void removeActiveButton(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                bubble[activeButton].setChecked(false);
                bubble[activeButton].setActivated(false);
                bubble[activeButton].setVisibility(View.INVISIBLE);
                numberOfButtons--;
                removeActiveButton = false;
                System.out.println("Removing active button...");


            }
        });
    }
    private void startRecording() {

        /*
         * This function sets up the AudioRecord class and creates a new thread to run the audio.
         * A new thread is required to allow button functionality which will break the record loop as required.
         * recorder.read() is used to read the audio from the hardware and print into audioData[].
         * audioData[] is the size of a single buffer. The info in audioData[] is then converted into
         * a complex array in preparation for FFT. FFT is then fed that data.
         */

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,   //setup the AudioRecord object.
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, RECORDER_BUFFERSIZE);
        final float[] audioData = new float[RECORDER_BUFFERSIZE]; //array to hold 1 buffer of audioData.

        isRecording = true;  //set bool to true for button functionality
        recorder.startRecording(); //start recording
        System.out.println("Bin Frequency Resolution: " + frequencyResolution);

        System.out.println("Starting to record...");

        recordingThread = new Thread(new Runnable() {   //this thread is created so that the buttons
            public void run() {                         //can run concurrently with the audio stream.

                while (isRecording) //loop for audio processing
                {
                    //read audio data from device and fill audioData[]
                    recorder.read(audioData, 0, RECORDER_BUFFERSIZE, AudioRecord.READ_NON_BLOCKING);

                    for (int i = 0; i < audioData.length; i++) {
                        emptyArray[i] = 0;//iterate through buffer and fill complex array with audio data.
                        realArray[i] = audioData[i];
                    }
                    fft.fft(realArray, emptyArray);//feed complex audio data into FFT object.
                    calculateFundamental();//run calculations of estimated fundamental frequency
                }
            }
        }, "AudioRecorder Thread");
        recordingThread.start();

    }

    private void stopRecording() {
        // stops the recording activity by setting everything to default states
        // NOTE: THREAD MUST BE CLOSED HERE.
        if (isRecording == true) {
            try {
                isRecording = false;
                recorder.stop();
                recorder.release();
                recorder = null;
                recordingThread = null;
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "error:" + e.getMessage());
            }
        }
    }

    private void bubbleStartLogic(int currentNoteId) {
        startRecording();
        mediaPlayer = MediaPlayer.create(this, audioId[currentNoteId]);
        mediaPlayer.start();
    }

    private void bubbleStopLogic() {

        stopRecording();
        try {
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "error:" + e.getMessage());
        }


    }

    public void OnClick(View v) {
        //if another button is pressed, turn that button off and stop recording/media player
        int activeId = v.getId();
        for (int i = 0; i < numberOfButtons; i++) {
            if (bubble[i].getId() != activeId) {
                bubble[i].setChecked(false);
                bubbleStopLogic();
            } else {
                activeButton = i;
            }

        }
        //if button is turned on, play sound and start recording
        if (bubble[activeButton].isChecked()) {
            bubbleStartLogic(activeButton);
        }

    }

    private View.OnClickListener pauseClick = new View.OnClickListener() {

        public void onClick(View v) {
            final AlertDialog.Builder mBuilder = new AlertDialog.Builder(GameActivity.this);
            View mView = getLayoutInflater().inflate(R.layout.dialog_pause_menu, null);
            TextView mPoints = (TextView) mView.findViewById(R.id.points);
            Button mPlay = (Button) mView.findViewById(R.id.resume);
            mBuilder.setView(mView);
            final AlertDialog dialog = mBuilder.create();
            timer.pause();
            bubbleStopLogic();
            dialog.show();
            mPlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dialog.hide();
                    timer.start();

                }
            });
        }
    };

    public synchronized double dataSmoothing(double value) {
        LinkedList values = new LinkedList();
        double sum = 0;

        double average;

        if (values.size() == avgCounter && avgCounter > 0) {
            sum -= ((Double) values.getFirst()).doubleValue();
            values.removeFirst();
            avgCounter--;
        }
        sum += value;
        values.addLast(new Double(value));
        average = sum / values.size();
        avgCounter++;
        return average;
    }

    private void calculateFundamental() {
        /*
        calculates the fundamental freq of the incoming signal by iterating through the fft
        array and determining which bin has the largest magnitude and therefore the most dominant frequency. Then the starting
        frequency of that bin is calculated and used as the fundamental value. So not quite the exact fundamental, but close enough.
        */

        int highestIndex = 0;
        double fundamentalMagnitude = 0;
        double currentValueReal;

        for (int i = lowestBinForVocals; i < numberOfBins; i++)//note that we don't start at 0 in the for() loop. This is because the fft
        {                                                       //constantly gets false data in the first couple of bins, most of these
            currentValueReal = realArray[i];                        //frequencies are lower than what we can hear.

            /*
             * iterates through each bin and will select the bin with the largest magnitude
             */

            if (currentValueReal > fundamentalMagnitude && currentValueReal > 5.0f) {
                fundamentalMagnitude = currentValueReal;
                highestIndex = i;
            }

        }
  
        // calculate the approximate fundamental frequency...
        fundamentalFrequency = highestIndex * frequencyResolution;
        averageFundamental = dataSmoothing(fundamentalFrequency);
        checkBubbles();
    }
    // onClick of backbutton finishes the activity.
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            if (isRecording) {
                try {
                    recorder.stop();
                    recorder.release();
                    isRecording = false;
                    recorder = null;
                    recordingThread = null;
                    finish();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "error:" + e.getMessage());
                }

            } else {
                finish();
            }

        }
        return super.onKeyDown(keyCode, event);
    }

}

package com.jonnymcgregor.android.singme;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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

import java.util.LinkedList;
import java.util.Random;


public class GameActivity extends AppCompatActivity {


    private static final int RECORDER_SAMPLERATE = 44100;

    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;

    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_FLOAT;

    private static final String TAG = null;

    private int RECORDER_BUFFERSIZE = 2048;
    // AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
    //RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
    private AudioRecord recorder = null;  //short array that pcm data is put into.

    private MediaPlayer mediaPlayer = null;

    private boolean isRecording = false;

    private Thread recordingThread = null;

    public float numberOfBins = 2048 / 2;

    public float frequencyResolution = RECORDER_SAMPLERATE / RECORDER_BUFFERSIZE;

    public int lowestBinForVocals = 200 / ((int) frequencyResolution);

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

    private int levelNo = 1;

    private int activeButton = 0;

    private int numberOfButtons = 3;

    private float noteArray[] = new float[13];

    private int audioArray[] = new int[13];

    private float buttonFreq[] = new float[10];

    public int avgCounter = 0;

    public double averageFundamental = 0;

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
        fft = new FFT (2048);
        mainLoop();

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

    private void checkBubbles() {
        //for (int i = 0; i < numberOfButtons; i++) {
            if (bubble[activeButton].isPressed() &&averageFundamental < (buttonFreq[activeButton] + 2*frequencyResolution)
                    && averageFundamental > (buttonFreq[activeButton] - 2*frequencyResolution)) {
                stopRecording();
               try{
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;} catch(Exception e){
                   e.printStackTrace();
                   Log.e(TAG, "error:" + e.getMessage());
               }

                bubble[activeButton].setActivated(false);
                numberOfButtons--;

            }
        //}
    }

    private void setLevelParameters() {
        numberOfButtons = 5;
        for (int i = 0; i < numberOfButtons; i++) {
            int random = new Random().nextInt(12);
            buttonFreq[i] = noteArray[random];
            audioId[i] = audioArray[random];

        }
        startTime = 30000;
    }

    private void startTimer() {
        timer = new CountDownTimerPausable(timeRemaining, 1000) {

            public void onTick(long millisUntilFinished) {
                timeRemaining = millisUntilFinished;
                timerText.setText("" + timeRemaining / 1000);
                //here you can have your logic to set text to edittext

            }

            public void onFinish() {
                timerText.setText("done!");
            }

        }.start();
    }

    private void mainLoop() {
        setLevelParameters();
        initialiseButtons();
        timeRemaining = startTime;
        startTimer();

            if (timeRemaining == 0) {
                //do game over stuff
            }
            if (numberOfButtons == 0) {
                //do win stuff
                levelNo++;
            }


    }

    private void startRecording() {

        /*
         * This function sets up the AudioRecord class and creates a new thread to run the audio.
         * A new thread is required to allow button functionality which will break the record loop as required.
         * recorder.read() is used to read the audio from the hardware and print into audioData[].
         * audioData[] is the size of a single buffer. The info in audioData[] is then converted into
         * a complex array in preparation for FFT. FFT is then fed that data.        *
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

                    //Complex[] complexData = new Complex[audioData.length]; //create complex array for audio data
                    for (int i = 0; i < audioData.length; i++) {
                        emptyArray[i] = 0;//iterate through buffer and fill complex array with audio data.
                        realArray[i] = audioData[i];
                        //System.out.println("AudioData: " + audioData[i]);
                    }
                    fft.fft(realArray, emptyArray);//feed complex audio data into FFT object.
                    //System.out.println("FFTData: " + fftResult[5]);
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
            }catch(Exception e){
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
            }catch(Exception e){
                e.printStackTrace();
                Log.e(TAG, "error:" + e.getMessage());
            }


    }

    public void OnClick(View v)
    {
        //if another button is pressed, turn that button off and stop recording/media player
        int activeId = v.getId();
        for(int i = 0;i < numberOfButtons; i++){
            if(bubble[i].getId() != activeId){
                bubble[i].setChecked(false);
                bubbleStopLogic();
            }
            else{
                activeButton = i;
            }

        }
        //if button is turned on, play sound and start recording
        if(bubble[activeButton].isChecked()){
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

    public synchronized double dataSmoothing(double value)
    {
        LinkedList values = new LinkedList();

        double sum = 0;

        double average = 0;

        if (values.size() == avgCounter && avgCounter > 0)
        {
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
        this function calculates the fundamental freq of the incoming signal by iterating through the fft
        array and determining which bin has the largest magnitude and therefore the most dominant frequency. Then the starting
        frequency of that bin is calculated and used as the fundamental value. So not quite the exact fundamental, but close enough.
        */

        int highestIndex = 0;
        double fundamentalMagnitude = 0;
        double currentValueReal = 0;

        for (int i = lowestBinForVocals; i < numberOfBins; i++)//note that we don't start at 0 in the for() loop. This is because the fft
        {                                                       //constantly gets shit data in the first couple of bins, most of these
            currentValueReal = realArray[i];                        //frequencies are lower than what we can hear.

            /*
             * this if() statement will iterate through every bin and, by process of elimination, will select the bin with the largest magnitude
             * to be the fundamentalMagnitude, the index of that bin will then be stored in highestIndex. When the loop is finished, highestIndex
             * will contain the bin value of the fundamental frequency.
             */

            if (currentValueReal > fundamentalMagnitude) {
                fundamentalMagnitude = currentValueReal;
                highestIndex = i;
            }

        }

        /*
         * The equation below takes the frequency resolution of a single bin and multiplies it by the index number
         * of fundamentalMagnitude, thus giving us the approximate fundamental frequency.
         */

        fundamentalFrequency = highestIndex * frequencyResolution;
        //System.out.println("Index of Fundamental: " + highestIndex + " Approx Frequency of Fundamental: " + fundamentalFrequency);
        averageFundamental = dataSmoothing(fundamentalFrequency);
        System.out.println("Averaged Fundamental = " + averageFundamental);
        dataSmoothing(fundamentalFrequency);
        checkBubbles();
        //average before you check bubbles
        //checks if fundamental frequency aligns with buttonFreq
    }

    // onClick of backbutton finishes the activity.
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            if (isRecording) {
                try{
                recorder.stop();
                recorder.release();
                isRecording = false;
                recorder = null;
                recordingThread = null;
                finish();}catch(Exception e){
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

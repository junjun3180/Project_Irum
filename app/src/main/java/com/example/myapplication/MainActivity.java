package com.example.myapplication;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private File[] mediaFiles;
    private int currentMediaIndex = 0; // 현재 재생 중인 미디어 인덱스
    private VideoView videoView;
    private ImageView imageView;
    private EditText timeInput;
    Button playButton;
    Button stopButton;

    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Edge-to-Edge 및 레이아웃 초기화
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // UI 요소 초기화
        videoView = findViewById(R.id.videoView);
        imageView = findViewById(R.id.imageView);
        timeInput = findViewById(R.id.timeInput);
        playButton = findViewById(R.id.playButton);
        stopButton = findViewById(R.id.stopButton);

        // 1. 미디어 폴더 생성 및 파일 로드
        createMediaFolder();
        loadMediaFiles();

        // 화면 종료 이벤트 설정(터치 시)
        videoView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                exitFullScreen();
                return true;
            }
            return false;
        });

        imageView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                exitFullScreen();
                return true;
            }
            return false;
        });

        // 2. 재생 버튼 클릭 이벤트
        playButton.setOnClickListener(v -> {
            // 키보드 닫기
            closeKeyboard();

            if (mediaFiles != null && mediaFiles.length > 0) {
                enterFullScreen();
                playMedia();
            } else {
                Toast.makeText(this, "미디어 파일이 없습니다.", Toast.LENGTH_LONG).show();
            }
        });

        // 3. 중지 버튼 클릭 이벤트
        stopButton.setOnClickListener(v -> stopMedia());
    }

    // 전체 화면 설정
    private void enterFullScreen() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        // 버튼 숨기기
        hideButtons();
    }

    private void exitFullScreen() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_VISIBLE // 상태 바와 내비게이션 바 표시
        );

        // 버튼 보이기
        showButtons();

        Toast.makeText(this, "전체 화면 모드 종료", Toast.LENGTH_SHORT).show();
    }

    // 버튼 숨기기
    private void hideButtons() {
        playButton.setVisibility(View.GONE);
        stopButton.setVisibility(View.GONE);
    }

    // 버튼 보이기
    private void showButtons() {
        playButton.setVisibility(View.VISIBLE);
        stopButton.setVisibility(View.VISIBLE);
    }

    private void createMediaFolder() {

        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        // 외부 저장소의 최상위 디렉토리에 "MyMediaFolder" 폴더 경로 설정
        File mediaFolder = new File(Environment.getExternalStorageDirectory(), "MyMediaFolder");

        // 폴더가 없는 경우 생성
        if (!mediaFolder.exists()) {
            boolean created = mediaFolder.mkdirs();
            if (created) {
                Toast.makeText(this, "미디어 폴더가 생성되었습니다: " + mediaFolder.getAbsolutePath(), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "미디어 폴더 생성 실패!", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void loadMediaFiles() {
        // 동일한 경로를 참조
        File mediaFolder = new File(Environment.getExternalStorageDirectory(), "MyMediaFolder");

        if (mediaFolder.exists()) {
            mediaFiles = mediaFolder.listFiles();
        }
    }

    // 미디어 재생 메서드
    private void playMedia() {
        if (mediaFiles == null || mediaFiles.length == 0) return;

        File currentFile = mediaFiles[currentMediaIndex];
        if (currentFile.getName().endsWith(".mp4")) {
            playVideo(currentFile);
        } else {
            playImage(currentFile);
        }
    }

    // 동영상 재생
    private void playVideo(File videoFile) {
        videoView.setVisibility(View.VISIBLE);
        imageView.setVisibility(View.GONE);

        videoView.setVideoURI(Uri.fromFile(videoFile));
        videoView.start();

        videoView.setOnCompletionListener(mp -> nextMedia());
    }

    // 사진 재생
    private void playImage(File imageFile) {
        videoView.setVisibility(View.GONE);
        imageView.setVisibility(View.VISIBLE);

        imageView.setImageURI(Uri.fromFile(imageFile));

        // 사진 표시 시간 가져오기
        int duration = getDurationFromInput();
        handler.postDelayed(this::nextMedia, duration * 1000);
    }

    // 다음 미디어로 이동
    private void nextMedia() {
        currentMediaIndex = (currentMediaIndex + 1) % mediaFiles.length; // 순환 재생
        playMedia();
    }

    // 사용자 입력 시간 가져오기
    private int getDurationFromInput() {
        String input = timeInput.getText().toString();
        try {
            return Integer.parseInt(input); // 입력된 값 반환
        } catch (NumberFormatException e) {
            return 5; // 기본값: 5초
        }
    }


    // 미디어 중지 메서드
    private void stopMedia() {
        // Handler에 예약된 작업 취소
        handler.removeCallbacksAndMessages(null);

        // 동영상 중지
        if (videoView.isPlaying()) {
            videoView.stopPlayback(); // 동영상 재생 중지
        }

        // 이미지 숨기기
        imageView.setVisibility(View.GONE);

        Toast.makeText(this, "재생이 중지되었습니다.", Toast.LENGTH_SHORT).show();
    }
}
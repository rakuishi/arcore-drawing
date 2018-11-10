package com.rakuishi.drawing;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Toast;

import com.google.ar.core.Pose;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.collision.Ray;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.ux.ArFragment;
import com.rakuishi.drawing.rendering.Stroke;

import java.util.ArrayList;
import java.util.concurrent.CompletionException;

public class MainActivity extends AppCompatActivity
        implements Scene.OnUpdateListener, Scene.OnPeekTouchListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final float DRAW_DISTANCE = 0.13f;
    private ArFragment arFragment;
    private Material material;
    private AnchorNode anchorNode;
    private final ArrayList<Stroke> strokes = new ArrayList<>();
    private Stroke currentStroke;

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        arFragment.getArSceneView().getPlaneRenderer().setEnabled(false);
        arFragment.getArSceneView().getScene().addOnUpdateListener(this);
        arFragment.getArSceneView().getScene().addOnPeekTouchListener(this);

        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.WHITE))
                .thenAccept(material1 -> material = material1.makeCopy())
                .exceptionally(this::handleMaterialError);
    }

    private Void handleMaterialError(Throwable throwable) {
        Toast toast = Toast.makeText(this, "Unable to create material", Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
        throw new CompletionException(throwable);
    }

    // region Scene.OnUpdateListener

    @Override
    public void onUpdate(FrameTime frameTime) {
        com.google.ar.core.Camera camera = arFragment.getArSceneView().getArFrame().getCamera();
        if (camera.getTrackingState() == TrackingState.TRACKING) {
            // Hide instructions for how to scan for planes
            arFragment.getPlaneDiscoveryController().hide();
        }
    }

    // endregion

    // region Scene.OnPeekTouchListener

    @Override
    public void onPeekTouch(HitTestResult hitTestResult, MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        Camera camera = arFragment.getArSceneView().getScene().getCamera();
        Ray ray = camera.screenPointToRay(motionEvent.getX(), motionEvent.getY());
        Vector3 drawPoint = ray.getPoint(DRAW_DISTANCE);
        Log.d(TAG, drawPoint.toString());

        if (action == MotionEvent.ACTION_DOWN) {
            if (anchorNode == null) {
                ArSceneView arSceneView = arFragment.getArSceneView();
                com.google.ar.core.Camera coreCamera = arSceneView.getArFrame().getCamera();
                if (coreCamera.getTrackingState() != TrackingState.TRACKING) {
                    return;
                }
                Pose pose = coreCamera.getPose();
                anchorNode = new AnchorNode(arSceneView.getSession().createAnchor(pose));
                anchorNode.setParent(arSceneView.getScene());
            }
            currentStroke = new Stroke(anchorNode, material);
            strokes.add(currentStroke);
            currentStroke.add(drawPoint);
        } else if (action == MotionEvent.ACTION_MOVE && currentStroke != null) {
            currentStroke.add(drawPoint);
        }

    }

    // endregion
}

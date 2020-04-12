package com.aswin.measurear;

import androidx.appcompat.app.AppCompatActivity;

import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.AnchorNotSupportedForHostingException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.collision.Ray;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.rendering.ViewSizer;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetector;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MainActivity extends AppCompatActivity  implements Scene.OnUpdateListener {

    private ArFragment arFragment;
    private List<AnchorNode> nodes;
    private List<AnchorNode> tempNodes;
    private AnchorNode anchorNode;
    private Point point;
    private Anchor anchor;
    private ModelRenderable renderable;
    private ModelRenderable nodeRenderable;
    private ViewRenderable viewRenderable;
    private AnchorNode line;
    private Node centreNode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Display  display =  getWindowManager().getDefaultDisplay();
        point = new Point();
        display.getRealSize(point);

        setContentView(R.layout.activity_main);

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arFragment);
        nodes = new ArrayList<>();
        tempNodes = new ArrayList<>();
        centreNode = new Node();
        line = new AnchorNode();

        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.RED))
                .thenAccept(material ->
                    renderable = ShapeFactory.makeSphere(0.01f,new Vector3(0.0f,0.0f,0.0f),material)

                );

        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.RED))
                .thenAccept(material ->
                   nodeRenderable = ShapeFactory.makeSphere(0.005f, new Vector3(0.0f, 0.0f, 0.0f),material)
                );





       arFragment.getArSceneView().getScene().addOnUpdateListener(this);


        Button addAnchor = findViewById(R.id.setAnchor);

        addAnchor.setOnClickListener(v -> {

            anchor = createAnchorNode();
            anchorNode = new AnchorNode(anchor);
            if(tempNodes != null && tempNodes.size() == 1) {

                anchorNode.setParent(arFragment.getArSceneView().getScene());
                anchorNode.setRenderable(nodeRenderable);

                AnchorNode prevNode = nodes.get(nodes.size()-1);

                float length = drawLine(prevNode,anchorNode);
                Vector3 midpoint = getMidPoint(prevNode,anchorNode);
                drawAtMidPoint(midpoint, length);

               // Toast.makeText(this, "Length of the side is "+length+"cm", Toast.LENGTH_LONG).show();
                nodes.add(anchorNode);
                tempNodes.clear();
            } else {
                anchorNode.setRenderable(nodeRenderable);
                anchorNode.setParent(arFragment.getArSceneView().getScene());
                nodes.add(anchorNode);
                tempNodes.add(anchorNode);
            }


        });
    }


    @Override
    public void onUpdate(FrameTime frameTime) {
       // Frame frame = arFragment.getArSceneView().getArFrame();
        Camera camera = arFragment.getArSceneView().getScene().getCamera();
        Ray ray;
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ray = camera.screenPointToRay(point.x/2.0f, point.y/2.0f);
        } else {

            ray = camera.screenPointToRay(point.x/2.0f, point.y/2.0f);
        }
        Vector3 newPosition = ray.getPoint(1.0f);
        centreNode.setLocalPosition(newPosition);
        centreNode.setRenderable(renderable);
        centreNode.setParent(arFragment.getArSceneView().getScene());



        if(tempNodes != null &&  tempNodes.size() >0){
           Anchor anchor = createAnchorNode();
            AnchorNode anchorNode = new AnchorNode(anchor);

                anchorNode.setParent(arFragment.getArSceneView().getScene());

                AnchorNode node1 = nodes.get(nodes.size() - 1);
                AnchorNode node2 = anchorNode;

                drawLine(node2,node1,true);
               // arFragment.getArSceneView().getScene().addChild(nodes.get(nodes.size()-1));





        }
    }

    @Override
    protected void onResume() {
        super.onResume();

    }


   public Anchor createAnchorNode(){

        Frame frame = arFragment.getArSceneView().getArFrame();
        Anchor localAnchor;
       List<HitResult> hitResults;
       try {
           hitResults =  frame.hitTest(point.x/2.0f, point.y/2.0f);

           if(hitResults != null && hitResults.size() > 0){
               localAnchor = hitResults.get(0).createAnchor();
               return localAnchor;
           }

        } catch (NullPointerException e){
            e.getMessage();
        }

        return null;
   }

   public Vector3 getMidPoint(AnchorNode node1, AnchorNode node2){

        Vector3 point1, point2;
        point1 = node1.getWorldPosition();
        point2 =node2.getWorldPosition();

        float x = (point1.x + point2.x)/2;
        float y = (point1.y + point2.y)/2;
        float z = (point1.z + point2.z)/2;

        Vector3 midpoint = new Vector3(x,y,z);

        return midpoint;
   }

   public void drawAtMidPoint(Vector3 point, float value){

        AnchorNode anchorNode = new AnchorNode();
        anchorNode.setLocalPosition(point);
        anchorNode.setParent(arFragment.getArSceneView().getScene());

       ViewRenderable.builder()
               .setView(this,R.layout.lengthlayout)
               .setSizer(view -> new Vector3(0.05f, 0.1f, 0.05f))
               .setHorizontalAlignment(ViewRenderable.HorizontalAlignment.CENTER)
               .build()
               .thenAccept(viewRenderable ->{
                   this.viewRenderable = viewRenderable;
                   this.viewRenderable.setShadowCaster(false);
                   this.viewRenderable.setShadowReceiver(false);

                   View view = this.viewRenderable.getView();
                   TextView textView = view.findViewById(R.id.lengthView);
                   textView.setText(scale(value, 2) +"cm");

                   anchorNode.setRenderable(viewRenderable);
               });


   }

   public float drawLine(AnchorNode node1, AnchorNode node2){
        Vector3 point1,point2;

        point1 = node1.getWorldPosition();
        point2 = node2.getWorldPosition();

       Vector3 difference = Vector3.subtract(point1,point2);
       Vector3 normalisedDifference  = difference.normalized();



       Quaternion rotation = Quaternion.lookRotation(normalisedDifference, Vector3.up());
       MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.RED))
               .thenAccept(material -> {
                   ModelRenderable renderable = ShapeFactory.makeCube(new Vector3(.001f,.001f,difference.length()),Vector3.zero(),material);
                   AnchorNode line = new AnchorNode();
                   line.setParent(node2);
                   line.setRenderable(renderable);

                   line.setWorldPosition(Vector3.add(point1,point2).scaled(0.5f));
                   line.setWorldRotation(rotation);

               });
       return difference.length()*100;

   }

    public void drawLine(AnchorNode node1, AnchorNode node2, boolean track){
        Vector3 point1,point2;

        point1 = node1.getWorldPosition();
        point2 = node2.getWorldPosition();

        Vector3 difference = Vector3.subtract(point1,point2);
        Vector3 normalisedDifference  = difference.normalized();



        Quaternion rotation = Quaternion.lookRotation(normalisedDifference, Vector3.up());
        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.RED))
                .thenAccept(material -> {
                    ModelRenderable renderable = ShapeFactory.makeCube(new Vector3(.0005f,.0005f,difference.length()),Vector3.zero(),material);

                    line.setParent(node2);
                    line.setRenderable(renderable);

                    line.setWorldPosition(Vector3.add(point1,point2).scaled(0.5f));
                    line.setWorldRotation(rotation);

                    if(track){
                        anchorNode.removeChild(line);
                        anchorNode.addChild(line);

                    }
                });

    }

    public float scale(float value, int decimalPlaces){

        BigDecimal bd = new BigDecimal(Float.toString(value));
        bd = bd.setScale(decimalPlaces, BigDecimal.ROUND_UP);
        return bd.floatValue();
    }
}

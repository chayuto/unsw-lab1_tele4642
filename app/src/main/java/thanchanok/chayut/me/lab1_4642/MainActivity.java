package thanchanok.chayut.me.lab1_4642;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends ActionBarActivity {

    //debug
    private final static String TAG = "MainActivity";

    public static final String SERVERIP = "131.215.136.203";
    public static final int SERVERPORT = 46420;

    //data handling
    private  int previousTimeStamp =0;
    private  int previousSeqNo  = -99;
    private ArrayList<Integer> timeDiffList = new ArrayList<Integer>() ;

    private Handler mHandler;
    DatagramSocket mClientSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler();

        try {
            mClientSocket = new DatagramSocket(40747);

            ListenPacket tListenPacket = new ListenPacket();
            tListenPacket.clientSocket = mClientSocket;
            Thread t = new Thread(tListenPacket);
            t.start();
        }
        catch (SocketException ex){
            ex.printStackTrace();
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void btn1OnClick(View view){

        Log.d(TAG,"btn1OnClick");


        //Clear Varibles
        previousTimeStamp =0;
        previousSeqNo  = -99;
        timeDiffList.clear();

        //Start Sending Thread
        SendPacket tSendPacket = new SendPacket();
        tSendPacket.clientSocket = mClientSocket;
        Thread t = new Thread(tSendPacket);
        t.start();
    }





    public void processIncomingBytes (byte[] packetBytes){
        //if packet is valid
        if(packetBytes.length == 1500){
            byte[] secondBytes = new byte[4];
            byte[] millSecBytes = new byte[4];
            int seqNo ;


            //extract time stamp from the packet
            System.arraycopy(packetBytes,0,secondBytes,0,4);
            System.arraycopy(packetBytes,0,secondBytes,4,4);
            seqNo = (int) packetBytes[8];

            int second = java.nio.ByteBuffer.wrap(secondBytes).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
            int millSecond = java.nio.ByteBuffer.wrap(millSecBytes).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();

            int timeStamp = second*1000 + millSecond;

            //Log incomging packet
            Log.d(TAG,String.format("TimeStamp Packet %d: %d <- %d.%d",seqNo,timeStamp,second,millSecond));

            if(previousSeqNo == seqNo -1)//if there is no skip
            {
                int timeDiff = timeStamp - previousTimeStamp;

                timeDiffList.add(timeDiff);
                //TODO: do something with the time difference

                mHandler.post(new Runnable() {
                    @Override
                    public void run () {

                        //TODO: more UI stuff here~
                        //send toast to UI
                        //Toast.makeText(MainActivity.this, bytesToHex(data), Toast.LENGTH_LONG).show();
                    }
                });


            }
            //store timeStamp and seqNo for next round of incoming bytes
            previousTimeStamp = timeStamp;
            previousSeqNo = seqNo;


        }
        else{
            Log.d(TAG,String.format("Return Packet of incorrect Length %d",packetBytes.length));
            Log.d(TAG,bytesToHex(packetBytes));
        }
    }

    private void analyseTimeDifference(){

        int itemCount = timeDiffList.size();

        if(itemCount!=0){

        }
        else
        {

        }
    }

    //region runnable
    public class SendPacket implements Runnable {

        public DatagramSocket clientSocket;
        @Override
        public void run() {
            Log.d(TAG,"SendPacketThread.run()");
            try {

                // send message
                InetAddress serverAddr = InetAddress.getByName(SERVERIP);

                byte[] sendData = new byte[1500];
                String password = "getmybw";


                byte[] passwordByte = password.getBytes("US-ASCII");

                System.arraycopy(passwordByte,0,sendData,0,passwordByte.length);
                sendData[8] = (byte) 0x12;


                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddr, SERVERPORT);
                clientSocket.send(sendPacket);

            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public class ListenPacket implements Runnable {

        public DatagramSocket clientSocket;
        @Override
        public void run() {
            Log.d(TAG,"ListenPacket.run()");
            try {
                //clientSocket = new DatagramSocket(40747);

                while(true)
                {
                    // get reply back from Pi
                    byte[] receiveData1 = new byte[1500];
                    final DatagramPacket receivePacket = new DatagramPacket(receiveData1, receiveData1.length);
                    clientSocket.receive(receivePacket);
                    Log.d(TAG,String.format("%s %d %d", receivePacket.getAddress().toString(), receivePacket.getPort(),receivePacket.getLength()));

                    final byte[] data = receivePacket.getData();


                    processIncomingBytes(data);


                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    //endregion


    //Utility for debug
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 3];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 3] = hexArray[v >>> 4];
            hexChars[j * 3 + 1] = hexArray[v & 0x0F];
            hexChars[j * 3 + 2] = ':';
        }
        return new String(hexChars);
    }

}

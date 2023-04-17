package com.example.blue_resetnew
//net import
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.ChartData
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.DefaultAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule
import kotlinx.coroutines.*
import android.widget.TextView


class MainActivity : AppCompatActivity() {

    private val TAG = "BluetoothGraph"
    private val PERMISSIONS_REQUEST_CODE = 1
    private val BLUETOOTH_REQUEST_CODE = 2

    private lateinit var chart1: LineChart
    private lateinit var data1: LineData
    private lateinit var dataSet1: LineDataSet
    private lateinit var chart2: LineChart
    private lateinit var data2: LineData
    private lateinit var dataSet2: LineDataSet
    private lateinit var chart3: LineChart
    private lateinit var data3: LineData
    private lateinit var dataSet3: LineDataSet
    private var datasave1 = ArrayList<Float>()
    private var datasave2 = ArrayList<Float>()
    private var datasave3 = ArrayList<Float>()

    private lateinit var textView1: TextView
    private lateinit var textView2: TextView
    private lateinit var textView3: TextView
    private lateinit var textView4: TextView

    private var bluetoothSocket: BluetoothSocket? = null
    private var connectedThread: BluetoothServer.ConnectedThread? = null

    private var serverSocket: BluetoothServerSocket? = null
    private var isRunning = false
    private val connectedThreads =  mutableListOf<BluetoothServer.ConnectedThread>()


    private var bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothServer = BluetoothServer(bluetoothAdapter)

    var count = 0

    // valueformatter로 바꿔서 에러 가능성있음;
    private var xAxisValueFormatter: ValueFormatter = object : DefaultAxisValueFormatter(0) {
        private val mFormat = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        override fun getFormattedValue(value: Float, axis: AxisBase?): String {
            return mFormat.format(Date(value.toLong()))
        }
    }
    var permissionlistener: PermissionListener = object : PermissionListener {
        override fun onPermissionGranted() {
            Toast.makeText(this@MainActivity, "권한 허가", Toast.LENGTH_SHORT).show()
        }

        override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
            Toast.makeText(this@MainActivity, "권한 허용을 하지 않으면 서비스를 이용할 수 없습니다.", Toast.LENGTH_SHORT)
                .show()
        }
    }


    private fun asyncPermissionCheck() =
        TedPermission.create()
            .setPermissionListener(permissionlistener)
            .setRationaleMessage("앱을 이용하기 위해서는 접근 권한이 필요합니다")
            .setDeniedMessage("앱에서 요구하는 권한설정이 필요합니다...\n [설정] > [권한] 에서 사용으로 활성화해주세요.")
            .setPermissions(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT
            ).check()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        chart1 = findViewById(R.id.chart1)
        data1 = LineData()
        dataSet1 = LineDataSet(mutableListOf(Entry(0f, 0f)), "Raw Signal")
        initLineChart1(chart1, data1, dataSet1, "R")

        chart2 = findViewById(R.id.chart2)
        data2 = LineData()
        dataSet2 = LineDataSet(mutableListOf(Entry(0f, 0f)), "Breath Signal")
        initLineChart(chart2, data2, dataSet2, "B")

        chart3 = findViewById(R.id.chart3)
        data3 = LineData()
        dataSet3 = LineDataSet(mutableListOf(Entry(0f, 0f)), "Heart Signal")
        initLineChart(chart3, data3, dataSet3, "H")

        // Check for permissions required to use Bluetooth
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
            != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
                ),
                PERMISSIONS_REQUEST_CODE
            )
            initBluetooth_try()

        } else {
            // Permissions granted, proceed with Bluetooth initialization
            initBluetooth_try()
        }
    }
    private fun initLineChart1(chart: LineChart, data: LineData, dataSet: LineDataSet, descText: String) {
        dataSet.color = Color.BLUE
        dataSet.setDrawCircles(false)
        //dataSet.enableDashedLine(5f,10f,0f)
        data.addDataSet(dataSet)
        chart.data = data
        chart.axisLeft.axisMinimum = -30f
        chart.axisLeft.axisMaximum = 30f
        chart.axisLeft.setDrawGridLines(false)
        chart.axisRight.isEnabled = false
        chart.xAxis.isEnabled = false
        val desc = Description()
        desc.text = descText
        chart.description = desc
    }

    private fun initLineChart(chart: LineChart, data: LineData, dataSet: LineDataSet, descText: String) {
        dataSet.color = Color.BLUE
        dataSet.setDrawCircles(false)
        //dataSet.enableDashedLine(5f,10f,0f)
        data.addDataSet(dataSet)
        chart.data = data
        chart.axisLeft.axisMinimum = -2f
        chart.axisLeft.axisMaximum = 2f
        chart.axisRight.isEnabled = false
        chart.xAxis.isEnabled = false
        val desc = Description()
        desc.text = descText
        chart.description = desc
    }
    private fun initBluetooth_try(){
        try {
            initBluetooth()
        } catch (e: IOException) {
            Timer().schedule(1000){
                initBluetooth_try()
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        // Close Bluetooth socket and disconnect from device
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket!!.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error closing Bluetooth socket", e)
            }
        }
        connectedThread?.cancel()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                initBluetooth()
            }
        }
    }
    private fun initBluetooth() {
        startServer()
    }

    private fun updateChart(data: List<Float>) {
        if (data.size >= 3) {
            // Add new data point to each chart's data list
            datasave1.add(data[0])
            datasave2.add(data[1])
            datasave3.add(data[2])

            // Add the new entries to each dataset
            val xValue = datasave1.size.toFloat() - 1f
            val entry1 = Entry(xValue, datasave1.last())
            dataSet1.addEntry(entry1)
            val entry2 = Entry(xValue, datasave2.last())
            dataSet2.addEntry(entry2)
            val entry3 = Entry(xValue, datasave3.last())
            dataSet3.addEntry(entry3)

            dataSet1.setDrawValues(false)
            dataSet2.setDrawValues(false)
            dataSet3.setDrawValues(false)
            // Notify the chart data has changed
            data1.notifyDataChanged()
            data2.notifyDataChanged()
            data3.notifyDataChanged()
            chart1.notifyDataSetChanged()
            chart2.notifyDataSetChanged()
            chart3.notifyDataSetChanged()

            // Invalidate the chart if datasave1 has reached 100 entries
            if (datasave1.size == 100) {
                chart1.invalidate()
                chart2.invalidate()
                chart3.invalidate()
            }

            // Clear data sets and notify data has changed if datasave1 has reached 100 entries
            if (datasave1.size == 100) {
                datasave1.clear()
                datasave2.clear()
                datasave3.clear()
                dataSet1.clear()
                dataSet2.clear()
                dataSet3.clear()
                data1.notifyDataChanged()
                data2.notifyDataChanged()
                data3.notifyDataChanged()
                chart1.notifyDataSetChanged()
                chart2.notifyDataSetChanged()
                chart3.notifyDataSetChanged()
            }

            // Set the visible range and move the chart to the latest entry
            chart1.setVisibleXRangeMaximum(100f)
            chart1.moveViewToX(xValue)
            chart2.setVisibleXRangeMaximum(100f)
            chart2.moveViewToX(xValue)
            chart3.setVisibleXRangeMaximum(100f)
            chart3.moveViewToX(xValue)

            val yMax = maxOf(datasave1.maxOrNull() ?: 0f)
            val yMin = minOf(datasave1.minOrNull() ?: 0f)
            chart1.axisLeft.axisMaximum = yMax + 1f
            chart1.axisLeft.axisMinimum = yMin - 1f
        }
    }

    /// 0413추가
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    fun startServer() {
        coroutineScope.launch {
            bluetoothServer.start()
        }
    }

    fun stopServer() {
        bluetoothServer.stop()
        coroutineScope.cancel()
    }

    ///
    inner class BluetoothServer(private val adapter: BluetoothAdapter) {

        private val TAG = "BluetoothServer"

        private var serverSocket: BluetoothServerSocket? = null
        private var isRunning = false
        private val connectedThreads = mutableListOf<ConnectedThread>()

        suspend fun start() {
            withContext(Dispatchers.IO) {
                isRunning = true
                asyncPermissionCheck()
                serverSocket = adapter.listenUsingRfcommWithServiceRecord("MyApp", UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66"))

                while (isRunning) {
                    try {
                        val socket = serverSocket!!.accept()
                        Log.d(TAG, "Client connected")
                        val connectedThread = ConnectedThread(socket)
                        connectedThreads.add(connectedThread)
                        connectedThread.start()
                    } catch (e: IOException) {
                        Log.e(TAG, "Error accepting client connection", e)
                        break
                    }
                }
            }
        }

        fun stop() {
            isRunning = false
            serverSocket?.close()
            for (thread in connectedThreads) {
                thread.cancel()
            }
            connectedThreads.clear()
        }

        inner class ConnectedThread(private val socket: BluetoothSocket) : Thread() {

            private val TAG = "ConnectedThread"
            private val unwrapedVitalSignList = mutableListOf<Float>()
            private val filteredBreathSignList = mutableListOf<Float>()
            private val filteredHeartSignList = mutableListOf<Float>()
            var floatArray = floatArrayOf()

            override fun run() {
                // Call the suspend version of the function from the overridden run() function
                runBlocking {
                    doRun()
                }
            }

            private suspend fun doRun() {
                try {
                    val inputStream = socket.inputStream
                    val buffer = ByteArray(1024)
                    var bytesRead: Int
                    while (true) {
                        bytesRead = inputStream.read(buffer)
                        if (bytesRead == -1) break
                        //val message = String(buffer, 0, bytesRead)
                        val byteArrayOutputStream = ByteArrayOutputStream()
                        byteArrayOutputStream.write(buffer)
                        seperatedData(byteArrayOutputStream)

                        val datas = getUnwrappedVitalSign()
                        for (i in datas.first.indices) {
                            val data: List<Float> = listOf(
                                datas.first[i],
                                datas.second[i], datas.third[i]
                            )
                            withContext(Dispatchers.Main) {
                                updateChart(data)
                                count ++
                                if (count == 500) {
                                    textView1 = findViewById(R.id.TVBR)
                                    textView2 = findViewById(R.id.TBHR)
                                    textView3 = findViewById(R.id.TVB)
                                    textView4 = findViewById(R.id.TVDM)

                                    textView1.setText(floatArray[3].toInt().toString())
                                    textView2.setText(floatArray[4].toInt().toString())
                                    if (floatArray[5].toInt() == 1) {
                                        textView3.setText("ON")
                                        textView3.setTextColor(Color.BLUE)
                                    }
                                    else {
                                        textView3.setText("OFF")
                                        textView3.setTextColor(Color.RED)
                                    }
                                    if (floatArray[6].toInt() == 1) {
                                        textView4.setText("ON")
                                        textView4.setTextColor(Color.BLUE)
                                    }
                                    else {
                                        textView4.setText("OFF")
                                        textView4.setTextColor(Color.RED)
                                    }
                                    count = 0
                                }
                            }
                        }
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Error handling client connection", e)
                } finally {
                    socket.close()
                    connectedThreads.remove(this)
                }
            }

            fun cancel() {
                socket.close()
            }

            fun seperatedData(VitalData : ByteArrayOutputStream){
                var dataByteArray = VitalData.toByteArray()
                var tt: ByteArray = dataByteArray
                //var length = tt.size
                var num_count = 20
                //var buffapend_count = 0
                var buffer : ByteBuffer
                //var buffer = ByteBuffer.wrap(tt,20,40) // create a ByteBuffer starting at index 50 with a length of 15 bytes
                //var builder = StringBuilder()

                while(num_count<100) {
                    try {
                        buffer = ByteBuffer.wrap(tt, num_count, num_count + 28).order(ByteOrder.LITTLE_ENDIAN)
                        num_count = num_count + 28
                        floatArray = floatArrayOf(
                            buffer.float, buffer.float, buffer.float,
                            buffer.float, buffer.float, buffer.float, buffer.float
                        )
                        val result = floatArray.joinToString(separator = ", ")
                        //Log.d(TAG, "Received message: $result")

                        setUnwrappedVitalSign(floatArray[0], floatArray[1], floatArray[2])
                    } catch (e: IOException){
                        Log.e(TAG, "Waiting now", e)
                    }
                }
            }

            fun setUnwrappedVitalSign(rawPhase: Float, filteredBreadth: Float, filteredHeart: Float) {
                // during 10 seconds
                if (unwrapedVitalSignList.size >= 50) {
                    unwrapedVitalSignList.removeAt(0)
                    filteredHeartSignList.removeAt(0)
                    filteredBreathSignList.removeAt(0)
                }
                unwrapedVitalSignList.add(rawPhase)
                filteredBreathSignList.add(filteredBreadth)
                filteredHeartSignList.add(filteredHeart)
            }
            fun getUnwrappedVitalSign(): Triple<List<Float>, List<Float>, List<Float>> {
                return Triple(unwrapedVitalSignList, filteredHeartSignList, filteredBreathSignList)
            }
        }
    }
}
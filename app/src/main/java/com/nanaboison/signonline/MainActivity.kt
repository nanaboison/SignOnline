package com.nanaboison.signonline

import android.Manifest
import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.Paint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception
import android.provider.MediaStore
import android.R.attr.path
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.print.PrintAttributes
import android.print.pdf.PrintedPdfDocument
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import com.google.android.material.snackbar.Snackbar
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks  {
//prod ad appId:
// prod banner ad:
    companion object{
        var signature = false
        var bitmapImage: Bitmap? = null
    }

    val EXTERNAL_FILE_PERM =100;

    internal var imageFileRoot =
        Environment.getExternalStorageDirectory().absoluteFile.toString() + "/SignOnline/Image/"

    internal var pdfFileRoot =
        Environment.getExternalStorageDirectory().absoluteFile.toString() + "/SignOnline/Pdf/"

    private lateinit var mInterstitialAd: InterstitialAd
    private lateinit var snackbar: Snackbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (this as AppCompatActivity).supportActionBar!!.hide()

        setContentView(R.layout.activity_main)

        // Initialise ads
        MobileAds.initialize(this, getString(R.string.ad_appID))
        val adRequest = AdRequest.Builder().build()
        mInterstitialAd = InterstitialAd(this)
        mInterstitialAd.adUnitId = getString(R.string.fullscreen_ad_id)

        mInterstitialAd.loadAd(AdRequest.Builder().build())
        bannerad.loadAd(adRequest)

        mInterstitialAd.adListener = object : AdListener() {
            override fun onAdClosed() {
                mInterstitialAd.loadAd(AdRequest.Builder().build())
            }
        }

        tvclear.paintFlags = Paint.UNDERLINE_TEXT_FLAG
        tvclear.setOnClickListener { view ->
          imgsign.clearSignature()
        }

        btnimage.setOnClickListener{view ->
            if (imgsign.mSignatureThere){
                if (EasyPermissions.hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                    if (mInterstitialAd.isLoaded) {
                        mInterstitialAd.show()
                    }
                    saveImage()
                } else {
                    EasyPermissions.requestPermissions(this, getString(R.string.rationale_memory),
                        EXTERNAL_FILE_PERM, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
              //  saveImage()
            }
        }

        btnpdf.setOnClickListener { view ->
            if (EasyPermissions.hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    if (mInterstitialAd.isLoaded) {
                        mInterstitialAd.show()
                    }
                    savePDF()
                }else {
                    Toast.makeText(this, "Sorry, your android version does not support saving signature in pdf", Toast.LENGTH_LONG).show()
                }
            } else {
                EasyPermissions.requestPermissions(this, getString(R.string.rationale_memory),
                    EXTERNAL_FILE_PERM, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        imgsign.setOnClickListener { view ->
           // signDialog();
        }
        snackbar = Snackbar.make(imgsign, "Signature Successfully Saved", Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction("Okay", View.OnClickListener { snackbar.dismiss() })
    }

    fun saveImage(){
        try {
            val file = getFile(imageFileRoot, "png")

            val fOut = FileOutputStream(file)

            val pictureBitmap = imgsign.getImage() // obtaining the Bitmap

            pictureBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, fOut) // saving the Bitmap to a file compressed as a JPEG with 85% compression rate
            fOut!!.flush() // Not really required
            fOut!!.close() // do not forget to close the stream

            MediaStore.Images.Media.insertImage(contentResolver,
                file.getAbsolutePath(),
                file.getName(),
                file.getName()
            )


            snackbar.show();
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun savePDF(){
        val pictureBitmap = imgsign.getImage() // obtaining the Bitmap
        try {
            val file = getFile(pdfFileRoot, "pdf")
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(pictureBitmap!!.width, pictureBitmap.height, 1).create()
            val page = document.startPage(pageInfo)

            val canvas = page.canvas
            imgsign.drawCanvas(canvas)
            document.finishPage(page)

            document.writeTo(FileOutputStream(file))

            document.close()

            snackbar.show();
        }   catch (e: Exception){
            e.printStackTrace()
        }
    }

    fun getFile(rootFolder: String, fileType: String): File {
        val fileFolder = File(rootFolder+File.separator) // folder to save
        val date = Date()
        val sdf1 = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        var fileName=""

        if (!fileFolder.getParentFile()!!.exists()) {
            fileFolder.getParentFile()!!.mkdirs()

            if(!fileFolder.exists()){
                fileFolder.mkdir()
                Log.w("fileFolderNew", fileFolder.absolutePath)
            }

            Log.w("fileFolder", fileFolder.absolutePath)
        }


        if(fileType.equals("png", false)){
            fileName = "SIGN"+sdf1.format(date)+".png"
        }else if(fileType.equals("pdf", false)){
           fileName = "SIGN"+sdf1.format(date)+".pdf"
        }

        val file = File(fileFolder, fileName) // the File to save

        if (!file.getParentFile()!!.exists()) {
            file.getParentFile()!!.mkdirs()
        }
        Log.w("fileName", file.absolutePath)
        if (!file.exists()) {
            file.createNewFile()
        }

        return file
    }

    fun getNumberOfFile(path: String, name: String, mainFileRoot: String): Int{
        var num = 0
        val nameExt = "." + MimeTypeMap.getFileExtensionFromUrl(path)
        var name = name
        name = name.substring(0, name.indexOf(nameExt))

        val root = File(mainFileRoot)
        val files = root.listFiles()
        if (files != null) {
            for (file in root.listFiles()!!) {
                val fileName = file.getName().toLowerCase()
                if (fileName.contains(name.toLowerCase())) {
                    num++
                }
            }
        }

        return num
    }

    /*fun signDialog(){
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.signature_layout)
        dialog.setTitle(getString(R.string.sign_here))


        val mSignatureView = dialog
            .findViewById(R.id.signatureView) as SignatureView


        (dialog.findViewById(R.id.clear) as Button)
            .setOnClickListener { mSignatureView.clearSignature() }

        (dialog.findViewById(R.id.done) as Button)
            .setOnClickListener {
                dialog.dismiss()
                val bitmap = mSignatureView.getImage()
                if (bitmap != null) {
                    bitmapImage = bitmap
                    signature = true
                    imgsign.setImageBitmap(bitmap)
                } else {
                    bitmapImage = bitmap
                    signature = false
                    imgsign.setImageBitmap(null)

                }
            }

        if (signature) {
            imgsign.setImageBitmap(bitmapImage)
        } else {
            mSignatureView.clearSignature()
        }

        dialog.show()
    }*/
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults,
            this
        )


    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {

        EasyPermissions.requestPermissions(this, getString(R.string.rationale_memory), EXTERNAL_FILE_PERM,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        Toast.makeText(this, "Great, You can now save your signature", Toast.LENGTH_LONG).show()
       // saveImage()
    }
}

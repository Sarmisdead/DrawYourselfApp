package sarmisdead.drawyourself

import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.app.Dialog
import android.app.appsearch.AppSearchResult.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.provider.FontsContractCompat.FontRequestCallback.RESULT_OK
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception


class MainActivity : AppCompatActivity() {


    companion object {
        private const val  CAMERA_PERMISSION_CODE = 1
        private const val CAMERA_REQUEST_CODE = 2
    }

    private var desenhoView : DesenhoView? = null
    private var minhaImageButtonCorAtual : ImageButton? = null
    var customProgressDialog : Dialog? = null

    val openGalleryLauncher: ActivityResultLauncher<Intent> =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        result ->
        if (result.resultCode == RESULT_OK && result.data != null){
            val imageBackground : ImageView = findViewById(R.id.iv_background)

            imageBackground.setImageURI(result.data?.data)
        }
    }

    val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            permissions ->
            permissions.entries.forEach{
                val permissionName = it.key
                val isGranted = it.value

                if(isGranted){
                    Toast.makeText( this@MainActivity, "Acesso Permitido",Toast.LENGTH_LONG).show()

                    val pickIntent = Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

                    openGalleryLauncher.launch(pickIntent)

                }else{
                    if(permissionName== Manifest.permission.READ_EXTERNAL_STORAGE){
                        Toast.makeText( this@MainActivity, "Acesso Negado",Toast.LENGTH_LONG).show()
                    }
                }
            }

        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        desenhoView = findViewById(R.id.desenho_view)
        desenhoView?.escolherTamanhoBrush(20.toFloat())

        val linearLayoutCores = findViewById<LinearLayout>(R.id.ll_paintcolors)

        minhaImageButtonCorAtual = linearLayoutCores[0] as ImageButton

        minhaImageButtonCorAtual!!.setImageDrawable(
            ContextCompat.getDrawable(this,R.drawable.paleta_pressionada)

        )

        val ib_brush : ImageButton = findViewById(R.id.ib_brush)
        ib_brush.setOnClickListener{
            showBrushSizeChooserDialog()
        }

        val ib_gallery : ImageButton = findViewById(R.id.ib_galeria)
        ib_gallery.setOnClickListener{
            requestStoragePermission()
        }

        val ib_undo : ImageButton = findViewById(R.id.ib_undo)
        ib_undo.setOnClickListener{
            desenhoView?.onClickUndo()
        }

        val ib_redo : ImageButton = findViewById(R.id.ib_redo)
        ib_redo.setOnClickListener{
            desenhoView?.onClickRedo()
        }

        val ib_save : ImageButton = findViewById(R.id.ib_save)
        ib_save.setOnClickListener{

            if(isReadStorageAllowed()){
                ShowProgressDialog()
                lifecycleScope.launch{
                    val flDrawingView : FrameLayout = findViewById(R.id.fl_desenho_view_container)

                    saveBitmapFile(getBitmapFromView(flDrawingView))
                }
            }
        }

        val ib_share : ImageButton = findViewById(R.id.ib_share)
        ib_share.setOnClickListener{
            val result: String = ""
            MediaScannerConnection.scanFile(this, arrayOf(result), null){
                    path, uri ->
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                shareIntent.type = "image/png"
                startActivity(Intent.createChooser(shareIntent, "Compartilhar"))
            }
        }

        val ib_camera : ImageButton = findViewById(R.id.ib_camera)
        ib_camera.setOnClickListener{
            if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            ) {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(intent, CAMERA_REQUEST_CODE)
            }else{
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == CAMERA_PERMISSION_CODE){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(intent, CAMERA_REQUEST_CODE)
            }else{
                Toast.makeText(this, "Negado", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK){
            if(requestCode == CAMERA_REQUEST_CODE){
                val imageBackground : ImageView = findViewById(R.id.iv_background)
                val photo : Bitmap = data!!.extras!!.get("data") as Bitmap
                imageBackground.setImageBitmap(photo)
            }

        }
    }

    private fun showBrushSizeChooserDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.seletor_tamanho_brush)
        brushDialog.setTitle("Tamanho Pincel: ")

        val btnPequeno : ImageButton = brushDialog.findViewById(R.id.ib_brush_pequeno)
        btnPequeno.setOnClickListener{
            desenhoView?.escolherTamanhoBrush(10.toFloat())
            brushDialog.dismiss()
        }

        val btnMedio : ImageButton = brushDialog.findViewById(R.id.ib_brush_medio)
        btnMedio.setOnClickListener{
            desenhoView?.escolherTamanhoBrush(20.toFloat())
            brushDialog.dismiss()
        }

        val btnGrande : ImageButton = brushDialog.findViewById(R.id.ib_brush_grande)
        btnGrande.setOnClickListener{
            desenhoView?.escolherTamanhoBrush(30.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()
    }

    fun corClicked(view: View){
        if (view !== minhaImageButtonCorAtual){
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            desenhoView?.setCor(colorTag)

            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.paleta_pressionada))

            minhaImageButtonCorAtual?.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.paleta))

            minhaImageButtonCorAtual = view
        }
    }

    private fun isReadStorageAllowed() : Boolean{
        val result = ContextCompat.checkSelfPermission(this,
            Manifest.permission.READ_EXTERNAL_STORAGE)

        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.READ_EXTERNAL_STORAGE)){
            showRationaleDialog("DrawYourself App", "DrawYourself App" +
            "precisa acessar sua galeria de fotos")
        }else{
            requestPermission.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE

            ))
        }
    }

    private fun getBitmapFromView(view: View) : Bitmap{
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if(bgDrawable != null){
            bgDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)

        return returnedBitmap
    }

    private suspend fun saveBitmapFile(myBitmap : Bitmap?): String{
        var result = ""
        withContext(Dispatchers.IO){
            if(myBitmap != null){
                try {
                    val bytes = ByteArrayOutputStream()
                    myBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    val files = File(externalCacheDir?.absoluteFile.toString()
                            + File.separator + "DrawYourselfApp_"
                            + System.currentTimeMillis() /1000 + ".png" )

                    val fileout = FileOutputStream(files)
                    fileout.write(bytes.toByteArray())
                    fileout.close()

                    result = files.absolutePath

                    runOnUiThread{
                        cancelProgressDialog()
                        if(result.isNotEmpty()){
                            Toast.makeText(this@MainActivity,
                                "Arquivo salvo com sucesso: $result", Toast.LENGTH_LONG).show()
                        }else{
                            Toast.makeText(this@MainActivity,
                                "Algo deu errado", Toast.LENGTH_LONG).show()
                        }
                    }


                }
                catch (e: Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    private fun ShowProgressDialog(){
        customProgressDialog = Dialog(this@MainActivity)

        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)

        customProgressDialog?.show()
    }

    private fun cancelProgressDialog(){
        if(customProgressDialog != null){
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }



    private fun showRationaleDialog( title : String, message: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }


}
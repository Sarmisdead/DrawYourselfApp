package sarmisdead.drawyourself

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

class DesenhoView(contexto: Context, atributo: AttributeSet) : View(contexto, atributo) {

    private var meuDesenhoPath : CustomPath? = null
    private var meuCanvasBitmap : Bitmap? = null
    private var meuDesenhoPaint : Paint? = null
    private var meuCanvasPaint : Paint? = null
    private var meuTamanhoBrush : Float = 0.toFloat()
    private var color = Color.BLACK
    private var canvas : Canvas? = null
    private var meuPaths = ArrayList<CustomPath>()
    private var undoPaths = ArrayList<CustomPath>()

    init {
        setUpDesenho()
    }

    fun onClickRedo(){
        if(undoPaths.size > 0){
            meuPaths.add(undoPaths.removeAt(undoPaths.size -1))
            invalidate()
        }
    }

    fun onClickUndo(){
        if(meuPaths.size > 0){
            undoPaths.add(meuPaths.removeAt(meuPaths.size -1))
            invalidate()
        }
    }

    private fun setUpDesenho(){
        meuDesenhoPaint = Paint()
        meuDesenhoPath = CustomPath(color,meuTamanhoBrush)
        meuDesenhoPaint!!.color = color
        meuDesenhoPaint!!.style = Paint.Style.STROKE
        meuDesenhoPaint!!.strokeJoin = Paint.Join.ROUND
        meuDesenhoPaint!!.strokeCap = Paint.Cap.ROUND
        meuCanvasPaint = Paint(Paint.DITHER_FLAG)

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int){
        super.onSizeChanged(w, h, oldw, oldh)
        meuCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(meuCanvasBitmap!!)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(meuCanvasBitmap!!,0f,0f,meuCanvasPaint)

        for(path in meuPaths){
            meuCanvasPaint!!.strokeWidth = path .brushThickness
            meuDesenhoPaint!!.color = path.color
            canvas.drawPath(path, meuDesenhoPaint!!)
        }


        if (!meuDesenhoPath!!.isEmpty) {
            meuCanvasPaint!!.strokeWidth = meuDesenhoPath!!.brushThickness
            meuDesenhoPaint!!.color = meuDesenhoPath!!.color
            canvas.drawPath(meuDesenhoPath!!, meuDesenhoPaint!!)
        }
    }


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val touchX = event?.x
        val touchY = event?.y

        when(event?.action){
            MotionEvent.ACTION_DOWN ->{
                meuDesenhoPath!!.color = color
                meuDesenhoPath!!.brushThickness = meuTamanhoBrush

                meuDesenhoPath!!.reset()
                if (touchX != null) {
                    if (touchY != null) {
                        meuDesenhoPath!!.moveTo(touchX, touchY)
                    }
                }
            }
            MotionEvent.ACTION_MOVE ->{
                if (touchX != null) {
                    if (touchY != null) {
                        meuDesenhoPath!!.lineTo(touchX, touchY)
                    }
                }
            }
            MotionEvent.ACTION_UP ->{
                meuPaths.add(meuDesenhoPath!!)
                meuDesenhoPath = CustomPath(color, meuTamanhoBrush)
            }else -> return false
        }
        invalidate()

        return true
    }

    fun escolherTamanhoBrush(novoTamanho : Float){
        meuTamanhoBrush = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, novoTamanho, resources.displayMetrics)
        meuDesenhoPaint!!.strokeWidth = meuTamanhoBrush
    }

    fun setCor(novaCor: String){
        color = Color.parseColor(novaCor)
        meuDesenhoPaint!!.color = color
    }


    internal inner class CustomPath(var color: Int, var brushThickness: Float) : Path(){

    }

}
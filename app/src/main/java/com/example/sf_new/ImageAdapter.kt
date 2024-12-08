import android.content.Context
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView

class ImageAdapter(private val context: Context, private val imageUris: List<Uri>) : BaseAdapter() {

    override fun getCount(): Int = imageUris.size

    override fun getItem(position: Int): Any = imageUris[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val imageView: ImageView = if (convertView == null) {
            ImageView(context).apply {
                layoutParams = ViewGroup.LayoutParams(200, 200)
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
        } else {
            convertView as ImageView
        }

        imageView.setImageURI(imageUris[position])
        return imageView
    }
}

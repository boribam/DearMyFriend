import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bbam.dearmyfriend.R

class ImageSliderAdapter(
    private val imageUris: List<String>,
    private val onImageClick: ((String) -> Unit)? = null // 클릭 리스너 (선택)
) : RecyclerView.Adapter<ImageSliderAdapter.ImageViewHolder>() {

    class ImageViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val imageView = ImageView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        return ImageViewHolder(imageView)
    }

    override fun getItemCount(): Int {
        return imageUris.size
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageUrl = imageUris[position]

        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.user) // 로딩 중 기본 이미지
                    .error(R.drawable.user) // 로드 실패 시 기본 이미지
                    .diskCacheStrategy(DiskCacheStrategy.ALL) // 캐싱 전략
            )
            .into(holder.imageView)

        // 이미지 클릭 리스너 설정 (선택사항)
        holder.imageView.setOnClickListener {
            onImageClick?.invoke(imageUrl)
        }
    }
}

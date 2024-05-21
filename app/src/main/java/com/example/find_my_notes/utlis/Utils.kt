import android.app.Dialog
import android.content.Context
import android.widget.Toast
import com.example.find_my_notes.R

object Utils {

    object Utils {
        fun showProgressBar(context: Context) {
            val dialog = Dialog(context)
            dialog.setContentView(R.layout.progress_bar)
            dialog.setCancelable(false)
            dialog.show()
        }

        fun hideProgressBar(dialog: Dialog) {
            dialog.dismiss()
        }
    }

    fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

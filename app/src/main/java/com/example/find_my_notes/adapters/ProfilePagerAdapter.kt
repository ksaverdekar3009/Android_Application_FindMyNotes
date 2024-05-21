import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.find_my_notes.fragments.MyInfoFragment
import com.example.find_my_notes.fragments.MyNotesFragment

class ProfilePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
        // Return the number of tabs
        return TAB_COUNT
    }

    override fun createFragment(position: Int): Fragment {
        // Return a new instance of your fragment for each position
        return when (position) {
            0 -> MyNotesFragment()
            1 -> MyInfoFragment()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }

    companion object {
        private const val TAB_COUNT = 2 // Number of tabs
    }
}

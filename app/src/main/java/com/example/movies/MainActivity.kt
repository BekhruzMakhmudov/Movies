package com.example.movies

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.style.LeadingMarginSpan
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.SearchView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

data class Movie(
    val id: Int,
    val title: String,
    val releaseDate: String,
    val posterPath: String,
    val rating: Double,
    val voteCount: Int,
    val overview: String
)

class MainActivity : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var navigationView: NavigationView
    private lateinit var recyclerView: RecyclerView
    private lateinit var movieAdapter: MovieAdapter
    private var isListView = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawerLayout = findViewById(R.id.drawer_layout)
        toggle = ActionBarDrawerToggle(
            this, drawerLayout, R.string.open, R.string.close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        navigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    getMovies("now_playing")
                }
                R.id.nav_popular -> {
                    getMovies("top_rated")
                }
                R.id.nav_favorites -> {
                    val client = OkHttpClient()
                    val apiKey = "33d0a0d4e77e50e96fbee78cd587515b"
                    val sharedPreferences = getSharedPreferences("favorite_movies", MODE_PRIVATE)
                    val favoriteMovieIds = sharedPreferences.getStringSet("favorites", mutableSetOf()) ?: mutableSetOf()
                    val movies: MutableList<Movie> = mutableListOf()
                    for (id in favoriteMovieIds) {
                        val url = "https://api.themoviedb.org/3/movie/$id?api_key=$apiKey"
                        val request = Request.Builder().url(url).build()
                        Thread {
                            val response: Response = client.newCall(request).execute()
                            val movieJson = JSONObject(response.body?.string()!!)
                            val movie = Movie(
                                id = movieJson.getInt("id"),
                                title = movieJson.getString("title"),
                                releaseDate = movieJson.getString("release_date"),
                                posterPath = movieJson.getString("poster_path"),
                                rating = movieJson.getDouble("vote_average"),
                                voteCount = movieJson.getInt("vote_count"),
                                overview = movieJson.getString("overview")
                            )
                            movies.add(movie)
                            runOnUiThread {
                                movieAdapter = MovieAdapter(movies, isListView)
                                recyclerView.adapter = movieAdapter
                            }
                        }.start()
                    }
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
        recyclerView = findViewById(R.id.recyclerViewMovies)
        recyclerView.layoutManager = LinearLayoutManager(this)
        getMovies("now_playing")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    getMovies("search",it)
                }
                return false
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    getMovies("search",it)
                }
                return false
            }
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_toggle_view -> {
                toggleView(item)
                true
            }
            android.R.id.home -> {
                toggle.onOptionsItemSelected(item)
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleView(item: MenuItem) {
        if (isListView) {
            recyclerView.layoutManager = GridLayoutManager(this, 2)
            item.icon = getDrawable(R.drawable.ic_grid)
        } else {
            recyclerView.layoutManager = LinearLayoutManager(this)
            item.icon = getDrawable(R.drawable.ic_list)
        }
        isListView = !isListView
        movieAdapter.setViewType(isListView)
    }

    private fun getMovies(type:String,query: String=""){
        val client = OkHttpClient()
        val apiKey = "33d0a0d4e77e50e96fbee78cd587515b"
        var url = "https://api.themoviedb.org/3/movie/$type?api_key=$apiKey"
        if(type=="search") url="https://api.themoviedb.org/3/search/movie?api_key=$apiKey&query=$query"
        val request = Request.Builder()
            .url(url)
            .build()
        Thread {
            val movies: MutableList<Movie> = mutableListOf()
            val response: Response = client.newCall(request).execute()
            val jsonObject = JSONObject(response.body?.string()!!)
            val moviesArray = jsonObject.getJSONArray("results")
            for (i in 0 until moviesArray.length()) {
                val movieJson = moviesArray.getJSONObject(i)
                val movie = Movie(
                    id = movieJson.getInt("id"),
                    title = movieJson.getString("title"),
                    releaseDate = movieJson.getString("release_date"),
                    posterPath = movieJson.getString("poster_path"),
                    rating = movieJson.getDouble("vote_average"),
                    voteCount = movieJson.getInt("vote_count"),
                    overview = movieJson.getString("overview")
                )
                movies.add(movie)
            }
            runOnUiThread {
                movieAdapter = MovieAdapter(movies, isListView)
                recyclerView.adapter = movieAdapter
            }
        }.start()
    }
}

class MovieAdapter(private var movies: List<Movie>, private var isListView: Boolean) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val VIEW_TYPE_LIST = 1
        private const val VIEW_TYPE_GRID = 2
    }
    fun setViewType(isListView: Boolean) {
        this.isListView = isListView
        notifyDataSetChanged()
    }
    override fun getItemViewType(position: Int): Int {
        return if (isListView) VIEW_TYPE_LIST else VIEW_TYPE_GRID
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_LIST) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_movie_list, parent, false)
            ListViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_movie_grid, parent, false)
            GridViewHolder(view)
        }
    }
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val movie = movies[position]
        if (holder is ListViewHolder) {
            holder.textViewTitle.text = movie.title
            holder.textViewReleaseDate.text = movie.releaseDate
            holder.textViewRating.text = "Rating: ${movie.rating}"
            holder.textViewVoteCount.text = "Votes: ${movie.voteCount}"
            val spannable = SpannableString(movie.overview)
            spannable.setSpan(LeadingMarginSpan.Standard(40, 0), 0, 1, 0)
            holder.textViewOverview.text = spannable
            Glide.with(holder.itemView.context)
                .load("https://image.tmdb.org/t/p/w500${movie.posterPath}")
                .into(holder.imageViewPoster)
            holder.itemView.findViewById<LinearLayout>(R.id.list_item).setOnClickListener {
                val intent = Intent(holder.itemView.context, MovieDetailActivity::class.java).apply {
                    putExtra("id", movie.id)
                    putExtra("title", movie.title)
                    putExtra("releaseDate", movie.releaseDate)
                    putExtra("rating",movie.rating)
                    putExtra("voteCount",movie.voteCount)
                    putExtra("overview",movie.overview)
                    putExtra("posterPath", movie.posterPath)
                }
                holder.itemView.context.startActivity(intent)
            }
        } else if (holder is GridViewHolder) {
            holder.textViewTitle.text = movie.title
            Glide.with(holder.itemView.context)
                .load("https://image.tmdb.org/t/p/w500${movie.posterPath}")
                .into(holder.imageViewPoster)
            holder.imageViewPoster.setOnClickListener {
                val intent = Intent(holder.itemView.context, MovieDetailActivity::class.java).apply {
                    putExtra("id", movie.id)
                    putExtra("title", movie.title)
                    putExtra("releaseDate", movie.releaseDate)
                    putExtra("rating",movie.rating)
                    putExtra("voteCount",movie.voteCount)
                    putExtra("overview",movie.overview)
                    putExtra("posterPath", movie.posterPath)
                }
                holder.itemView.context.startActivity(intent)
            }
        }
    }
    override fun getItemCount(): Int = movies.size
    class ListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewPoster: ImageView = itemView.findViewById(R.id.imageViewPoster)
        val textViewTitle: TextView = itemView.findViewById(R.id.textViewTitle)
        val textViewReleaseDate: TextView = itemView.findViewById(R.id.textViewReleaseDate)
        val textViewRating:TextView = itemView.findViewById(R.id.textViewRating)
        val textViewVoteCount: TextView = itemView.findViewById(R.id.textViewVoteCount)
        val textViewOverview: TextView = itemView.findViewById(R.id.textViewOverview)
    }
    class GridViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewPoster: ImageView = itemView.findViewById(R.id.imageViewPoster)
        val textViewTitle: TextView = itemView.findViewById(R.id.textViewTitle)
    }
}

package com.example.movies

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.style.LeadingMarginSpan
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import okhttp3.*
import org.json.JSONObject

class MovieDetailActivity : AppCompatActivity() {
    private lateinit var movieId: String
    private lateinit var imageViewPoster: ImageView
    private lateinit var textViewTitle: TextView
    private lateinit var textViewReleaseDate: TextView
    private lateinit var textViewRating: TextView
    private lateinit var textViewVoteCount: TextView
    private lateinit var textViewOverview: TextView
    private lateinit var recyclerViewSimilarMovies: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movie_detail)
        movieId = intent.getIntExtra("id", 0).toString()
        val movieTitle = intent.getStringExtra("title")
        val movieReleaseDate = intent.getStringExtra("releaseDate")
        val movieRating = intent.getDoubleExtra("rating",0.0)
        val movieVoteCount = intent.getIntExtra("voteCount",0)
        val movieOverview = intent.getStringExtra("overview")
        val moviePosterPath = intent.getStringExtra("posterPath")
        imageViewPoster = findViewById(R.id.imageViewPoster)
        textViewTitle = findViewById(R.id.textViewTitle)
        textViewReleaseDate = findViewById(R.id.textViewReleaseDate)
        textViewRating = findViewById(R.id.textViewRating)
        textViewVoteCount = findViewById(R.id.textViewVoteCount)
        textViewOverview = findViewById(R.id.textViewOverview)
        recyclerViewSimilarMovies = findViewById(R.id.recyclerViewSimilarMovies)
        recyclerViewSimilarMovies.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val apiKey = "33d0a0d4e77e50e96fbee78cd587515b"
        val url = "https://api.themoviedb.org/3/movie/$movieId/similar?api_key=$apiKey"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
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
                textViewTitle.text=movieTitle
                textViewReleaseDate.text=movieReleaseDate
                textViewRating.text="Rating: ${movieRating}"
                textViewVoteCount.text="Votes: ${movieVoteCount}"
                val spannable = SpannableString(movieOverview)
                spannable.setSpan(LeadingMarginSpan.Standard(40, 0), 0, 1, 0)
                textViewOverview.text = spannable
                Glide.with(this@MovieDetailActivity)
                    .load("https://image.tmdb.org/t/p/w500$moviePosterPath")
                    .into(imageViewPoster)
                recyclerViewSimilarMovies.adapter = CarouselMoviesAdapter(movies)
            }
        }.start()
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_movie_detail, menu)
        val favoriteMenuItem = menu?.findItem(R.id.action_favorite)!!
        val sharedPreferences = getSharedPreferences("favorite_movies", MODE_PRIVATE)
        val favorites = sharedPreferences.getStringSet("favorites", mutableSetOf()) ?: mutableSetOf()
        if(favorites.contains(movieId)){
            favoriteMenuItem.setIcon(R.drawable.ic_star)
        } else{
            favoriteMenuItem.setIcon(R.drawable.ic_star_border)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_favorite -> {
                val sharedPreferences = getSharedPreferences("favorite_movies", MODE_PRIVATE)
                val favorites = sharedPreferences.getStringSet("favorites", mutableSetOf()) ?: mutableSetOf()
                val editor = sharedPreferences.edit()
                if (favorites.contains(movieId)) {
                    favorites.remove(movieId)
                    item.setIcon(R.drawable.ic_star_border)
                } else {
                    favorites.add(movieId)
                    item.setIcon(R.drawable.ic_star)
                }
                editor.putStringSet("favorites", favorites)
                editor.apply()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
class CarouselMoviesAdapter(private val movies: List<Movie>) : RecyclerView.Adapter<CarouselMoviesAdapter.MovieViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_movie_carousel, parent, false)
        return MovieViewHolder(view)
    }
    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val movie = movies[position]
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
    override fun getItemCount(): Int = movies.size
    class MovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageViewPoster: ImageView = itemView.findViewById(R.id.imageViewPoster)
        val textViewTitle: TextView = itemView.findViewById(R.id.textViewTitle)
    }
}

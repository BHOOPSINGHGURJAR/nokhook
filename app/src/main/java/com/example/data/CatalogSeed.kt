package com.example.data

import com.example.R
import com.example.model.AuthorProfile
import com.example.model.Book
import com.example.model.BookNewsItem
import com.example.model.BookSummaryItem
import com.example.model.CuratedCollectionSeed
import com.example.model.ExternalPlatformLink
import com.example.model.FollowedActivityItem
import com.example.model.PublisherProfile
import com.example.model.RatingMeterStats
import com.example.model.RatingTier
import com.example.model.RecommendationPerson
import com.example.model.SeedDiscussion
import com.example.model.SeedReaderProfile
import com.example.model.SeedReply
import com.example.model.SeedReview
import com.example.model.TimeFilter
import com.example.model.VibeSlice
import com.example.model.WhatsNewItem

object CatalogSeed {

    val authors = listOf(
        AuthorProfile(
            id = "auth_meera",
            name = "Meera Deshmukh",
            country = "India",
            bio = "Pune-based novelist known for coastal gothic mysteries and layered psychological suspense.",
            followersLabel = "48.2K Followers",
            accentColorHex = 0xFF1F3A5F,
            avatarRes = R.drawable.img_portrait_meera
        ),
        AuthorProfile(
            id = "auth_arjun",
            name = "Arjun Malhotra",
            country = "India",
            bio = "Behavioral economist and essayist exploring attention, craft, and modern urban life.",
            followersLabel = "62.5K Followers",
            accentColorHex = 0xFF2F6B55,
            avatarRes = R.drawable.img_portrait_arjun
        ),
        AuthorProfile(
            id = "auth_haruki",
            name = "Kenji Takahashi",
            country = "Japan",
            bio = "Master of surreal Tokyo realism, jazz-infused solitude, and melancholic coming-of-age stories.",
            followersLabel = "94.1K Followers",
            accentColorHex = 0xFF6B2D5C,
            avatarRes = R.drawable.img_portrait_kenji
        ),
        AuthorProfile(
            id = "auth_clara",
            name = "Clara Vance",
            country = "United Kingdom",
            bio = "Booker-shortlisted literary chronicler of memory, sea voyages, and quiet family reckonings.",
            followersLabel = "71.8K Followers",
            accentColorHex = 0xFF8C4A27,
            avatarRes = R.drawable.img_portrait_clara
        ),
        AuthorProfile(
            id = "auth_shrilal",
            name = "Raghavendra Joshi",
            country = "India",
            bio = "Sahitya Akademi laureate writing razor-sharp Hindi satire and North Indian small-town epics.",
            followersLabel = "39.4K Followers",
            accentColorHex = 0xFF7C3AED,
            avatarRes = R.drawable.img_portrait_raghavendra
        ),
        AuthorProfile(
            id = "auth_sora",
            name = "Sora Fujimoto",
            country = "Japan",
            bio = "Creator of the acclaimed Aether Chronicles manga and atmospheric world-building artbooks.",
            followersLabel = "128K Followers",
            accentColorHex = 0xFF0D9488,
            avatarRes = R.drawable.img_portrait_kenji
        ),
        AuthorProfile(
            id = "auth_kavya",
            name = "Kavya Iyer",
            country = "India",
            bio = "Founder turned startup chronicler and romance-comedy author celebrating ambitious women.",
            followersLabel = "53.0K Followers",
            accentColorHex = 0xFFC24D2C,
            avatarRes = R.drawable.img_portrait_meera
        )
    )

    val publishers = listOf(
        PublisherProfile(
            id = "pub_penguin",
            name = "Penguin Literary",
            headquarters = "New Delhi & London",
            description = "Flagship imprint publishing prize-winning literary fiction, essays, and South Asian translations.",
            accentColorHex = 0xFFEA580C
        ),
        PublisherProfile(
            id = "pub_rajkamal",
            name = "Rajkamal Heritage",
            headquarters = "New Delhi",
            description = "Premier Hindi and Marathi literary house preserving modern classics and contemporary voices.",
            accentColorHex = 0xFF7C3AED
        ),
        PublisherProfile(
            id = "pub_harper",
            name = "Harper Horizon",
            headquarters = "Mumbai & New York",
            description = "Home to breakout thrillers, business strategy bestsellers, and bold debuts.",
            accentColorHex = 0xFF1D4ED8
        ),
        PublisherProfile(
            id = "pub_shueisha",
            name = "Kurogane Comics",
            headquarters = "Tokyo",
            description = "Curated manga, graphic novels, and serialized dark-fantasy editions.",
            accentColorHex = 0xFF0F766E
        )
    )

    private fun defaultLinks(queryTitle: String): List<ExternalPlatformLink> {
        val q = queryTitle.replace(" ", "+")
        return listOf(
            ExternalPlatformLink("Amazon", "Buy Paperback", "https://www.amazon.in/s?k=$q"),
            ExternalPlatformLink("Flipkart", "Check Price", "https://www.flipkart.com/search?q=$q"),
            ExternalPlatformLink("Kindle", "Read Sample", "https://www.amazon.in/s?k=$q+kindle"),
            ExternalPlatformLink("Audible", "Listen Audio", "https://www.audible.in/search?keywords=$q")
        )
    }

    val seedBooks = listOf(
        // --- UPCOMING BOOKS (Most Interested Leaderboard) ---
        Book(
            id = "book_glass_monolith",
            title = "The Glass Monolith",
            subtitle = "A locked-observatory psychological thriller",
            authorId = "auth_meera",
            authorName = "Meera Deshmukh",
            publisherId = "pub_harper",
            publisherName = "Harper Horizon",
            year = 2027,
            pages = 384,
            estimatedHours = 6,
            language = "English",
            country = "India",
            ageRating = "16+",
            genre = "Thriller",
            subGenre = "Psychological Mystery",
            awards = listOf("Most Anticipated 2027"),
            overview = "When five glaciologists wake up inside a mirrored Himalayan research dome with forty-eight hours of missing memory, every instrument points to an impossible sixth survivor. Meera Deshmukh weaves claustrophobic tension with razor-sharp human psychology.",
            vibes = listOf(
                VibeSlice("Suspense", 45, 0xFF4F46E5),
                VibeSlice("Psychology", 35, 0xFFE05D44),
                VibeSlice("Mystery", 20, 0xFF0D9488)
            ),
            ratingStats = RatingMeterStats(420, 210, 45, 18),
            coverRes = R.drawable.img_book_cover_3,
            accentColorHex = 0xFF1E3A5F,
            isAppSelect = true,
            isUpcoming = true,
            releaseDateLabel = "Jan 18, 2027",
            interestedByFilter = mapOf(
                TimeFilter.THIS_WEEK to 11240,
                TimeFilter.THIS_MONTH to 34800,
                TimeFilter.THIS_QUARTER to 61200,
                TimeFilter.THIS_YEAR to 92400,
                TimeFilter.ALL_TIME to 118500
            ),
            buyLinks = defaultLinks("The Glass Monolith")
        ),
        Book(
            id = "book_monsoon_malgudi",
            title = "Monsoon Over Malgudi",
            subtitle = "Interlocking tales of rain, memory, and letters",
            authorId = "auth_clara",
            authorName = "Clara Vance",
            publisherId = "pub_penguin",
            publisherName = "Penguin Literary",
            year = 2026,
            pages = 296,
            estimatedHours = 5,
            language = "English",
            country = "UK",
            ageRating = "All Ages",
            genre = "Fiction",
            subGenre = "Literary Drama",
            awards = listOf("Editors' Watchlist"),
            overview = "A postmaster discovers an undelivered trunk of 1974 postcards in a crumbling veranda, sparking a tender journey across three generations of dreamers waiting for the first monsoon train.",
            vibes = listOf(
                VibeSlice("Drama", 50, 0xFFE05D44),
                VibeSlice("Nostalgia", 30, 0xFFD97706),
                VibeSlice("Romance", 20, 0xFF9333EA)
            ),
            ratingStats = RatingMeterStats(310, 190, 62, 12),
            coverRes = R.drawable.img_book_cover_2,
            accentColorHex = 0xFF2F6B55,
            isAppSelect = true,
            isUpcoming = true,
            releaseDateLabel = "Dec 04, 2026",
            interestedByFilter = mapOf(
                TimeFilter.THIS_WEEK to 8940,
                TimeFilter.THIS_MONTH to 38100,
                TimeFilter.THIS_QUARTER to 54900,
                TimeFilter.THIS_YEAR to 79200,
                TimeFilter.ALL_TIME to 96400
            ),
            isBeginnerFriendly = true,
            buyLinks = defaultLinks("Monsoon Over Malgudi")
        ),
        Book(
            id = "book_echoes_benares",
            title = "काशी के प्रतिध्वनि (Echoes of Benares)",
            subtitle = "घाटों, संगीत और इतिहास की अमर गाथा",
            authorId = "auth_shrilal",
            authorName = "Raghavendra Joshi",
            publisherId = "pub_rajkamal",
            publisherName = "Rajkamal Heritage",
            year = 2026,
            pages = 340,
            estimatedHours = 6,
            language = "Hindi",
            country = "India",
            ageRating = "13+",
            genre = "Fiction",
            subGenre = "Historical Saga",
            awards = listOf("Sahitya Buzz Pick"),
            overview = "Set along the lantern-lit ghats of 1950s Benares, a young tabla prodigy uncovers a forgotten classical composition that binds rival musical gharanas.",
            vibes = listOf(
                VibeSlice("Drama", 45, 0xFFE05D44),
                VibeSlice("Culture", 35, 0xFFD97706),
                VibeSlice("Psychology", 20, 0xFF4F46E5)
            ),
            ratingStats = RatingMeterStats(280, 140, 35, 9),
            coverRes = R.drawable.img_book_cover_1,
            accentColorHex = 0xFF6B2D5C,
            isUpcoming = true,
            releaseDateLabel = "Dec 19, 2026",
            interestedByFilter = mapOf(
                TimeFilter.THIS_WEEK to 7620,
                TimeFilter.THIS_MONTH to 26400,
                TimeFilter.THIS_QUARTER to 68400,
                TimeFilter.THIS_YEAR to 85100,
                TimeFilter.ALL_TIME to 104300
            ),
            buyLinks = defaultLinks("Echoes of Benares")
        ),
        Book(
            id = "book_zero_quiet",
            title = "Zero to Quiet",
            subtitle = "Reclaiming deep focus in an algorithmic age",
            authorId = "auth_arjun",
            authorName = "Arjun Malhotra",
            publisherId = "pub_harper",
            publisherName = "Harper Horizon",
            year = 2027,
            pages = 248,
            estimatedHours = 4,
            language = "English",
            country = "USA",
            ageRating = "All Ages",
            genre = "Self-Help",
            subGenre = "Productivity & Psychology",
            awards = listOf("Pre-Order #1 Business"),
            overview = "A practical field manual combining cognitive neuroscience and slow-work rituals to help builders, students, and creators rebuild uninterrupted thinking.",
            vibes = listOf(
                VibeSlice("Psychology", 55, 0xFF4F46E5),
                VibeSlice("Clarity", 30, 0xFF2F855A),
                VibeSlice("Business", 15, 0xFFD97706)
            ),
            ratingStats = RatingMeterStats(210, 175, 48, 15),
            coverRes = R.drawable.img_book_cover_2,
            accentColorHex = 0xFFC24D2C,
            isUpcoming = true,
            releaseDateLabel = "Feb 02, 2027",
            interestedByFilter = mapOf(
                TimeFilter.THIS_WEEK to 6410,
                TimeFilter.THIS_MONTH to 21900,
                TimeFilter.THIS_QUARTER to 49200,
                TimeFilter.THIS_YEAR to 110500,
                TimeFilter.ALL_TIME to 112000
            ),
            isBeginnerFriendly = true,
            buyLinks = defaultLinks("Zero to Quiet")
        ),
        Book(
            id = "book_aether_vol9",
            title = "Aether Chronicles Vol. 9",
            subtitle = "The Eclipse Citadel Arc",
            authorId = "auth_sora",
            authorName = "Sora Fujimoto",
            publisherId = "pub_shueisha",
            publisherName = "Kurogane Comics",
            year = 2026,
            pages = 212,
            estimatedHours = 2,
            language = "Japanese",
            country = "Japan",
            ageRating = "13+",
            genre = "Manga",
            subGenre = "Fantasy Action",
            awards = listOf("Manga Readers' Choice"),
            overview = "As the twin moons align above the Eclipse Citadel, Kael must forge an alliance with the sky-cartographers before the ancient ink seals shatter.",
            vibes = listOf(
                VibeSlice("Adventure", 50, 0xFF0D9488),
                VibeSlice("Drama", 30, 0xFFE05D44),
                VibeSlice("Mystery", 20, 0xFF4F46E5)
            ),
            ratingStats = RatingMeterStats(540, 205, 42, 11),
            coverRes = R.drawable.img_book_cover_1,
            accentColorHex = 0xFF0D9488,
            isAppSelect = true,
            isUpcoming = true,
            releaseDateLabel = "Nov 28, 2026",
            interestedByFilter = mapOf(
                TimeFilter.THIS_WEEK to 9850,
                TimeFilter.THIS_MONTH to 29700,
                TimeFilter.THIS_QUARTER to 51800,
                TimeFilter.THIS_YEAR to 98600,
                TimeFilter.ALL_TIME to 145000
            ),
            isSeriesOrManga = true,
            seriesLabel = "Aether Chronicles #9",
            isBeginnerFriendly = true,
            buyLinks = defaultLinks("Aether Chronicles Manga")
        ),

        // --- RELEASED BOOKS (Talk of the Town, Editor's Pick, Top 100, Awards, Languages) ---
        Book(
            id = "book_midnight_lighthouse",
            title = "The Midnight Lighthouse",
            subtitle = "Some signals are never meant to be answered",
            authorId = "auth_clara",
            authorName = "Clara Vance",
            publisherId = "pub_penguin",
            publisherName = "Penguin Literary",
            year = 2025,
            pages = 320,
            estimatedHours = 5,
            language = "English",
            country = "UK",
            ageRating = "13+",
            genre = "Fiction",
            subGenre = "Atmospheric Drama",
            awards = listOf("Booker Shortlist", "NYT Bestseller"),
            overview = "On a storm-lashed Hebridean headland, archivist Nora Thorne restores a 1920s lighthouse logbook—only to find entries written in her own handwriting dated thirty years before her birth. A lush, haunting meditation on grief, love, and parallel lives.",
            vibes = listOf(
                VibeSlice("Drama", 50, 0xFFE05D44),
                VibeSlice("Psychology", 35, 0xFF4F46E5),
                VibeSlice("Romance", 15, 0xFF9333EA)
            ),
            ratingStats = RatingMeterStats(1840, 920, 210, 48),
            coverRes = R.drawable.img_book_cover_1,
            accentColorHex = 0xFF182C4C,
            isAppSelect = true,
            isTalkOfTown = true,
            isEditorsPick = true,
            monthlyRank = 1,
            buyLinks = defaultLinks("The Midnight Lighthouse")
        ),
        Book(
            id = "book_shadows_shimla",
            title = "Shadows of Shimla",
            subtitle = "Inspector Deshmukh Case #1",
            authorId = "auth_meera",
            authorName = "Meera Deshmukh",
            publisherId = "pub_harper",
            publisherName = "Harper Horizon",
            year = 2024,
            pages = 356,
            estimatedHours = 6,
            language = "English",
            country = "India",
            ageRating = "16+",
            genre = "Thriller",
            subGenre = "Detective Noir",
            awards = listOf("Crossword Jury Pick"),
            overview = "Mist blankets the cedar ridges of Shimla as a retired magistrate vanishes during a winter snowstorm. Inspector Deshmukh unravels decades of buried ledgers in a twisting classic whodunit.",
            vibes = listOf(
                VibeSlice("Mystery", 45, 0xFF0D9488),
                VibeSlice("Suspense", 40, 0xFF4F46E5),
                VibeSlice("Drama", 15, 0xFFE05D44)
            ),
            ratingStats = RatingMeterStats(1290, 840, 180, 52),
            coverRes = R.drawable.img_book_cover_3,
            accentColorHex = 0xFF1F3D36,
            isAppSelect = true,
            isTalkOfTown = true,
            isSeriesOrManga = true,
            seriesLabel = "Inspector Deshmukh #1",
            monthlyRank = 2,
            buyLinks = defaultLinks("Shadows of Shimla")
        ),
        Book(
            id = "book_silent_algorithm",
            title = "The Quiet Compound",
            subtitle = "Tiny compounding habits for creative builders",
            authorId = "auth_arjun",
            authorName = "Arjun Malhotra",
            publisherId = "pub_harper",
            publisherName = "Harper Horizon",
            year = 2025,
            pages = 264,
            estimatedHours = 4,
            language = "English",
            country = "India",
            ageRating = "All Ages",
            genre = "Business",
            subGenre = "Strategy & Self-Help",
            awards = listOf("FT Business Book Pick"),
            overview = "Why small, unhurried daily systems beat dramatic sprints. Packed with founder case studies from Bengaluru to Tokyo, Arjun Malhotra charts a sustainable blueprint for deep mastery.",
            vibes = listOf(
                VibeSlice("Business", 45, 0xFFD97706),
                VibeSlice("Psychology", 35, 0xFF4F46E5),
                VibeSlice("Clarity", 20, 0xFF2F855A)
            ),
            ratingStats = RatingMeterStats(980, 1120, 290, 64),
            coverRes = R.drawable.img_book_cover_2,
            accentColorHex = 0xFF2F6B55,
            isTalkOfTown = true,
            isEditorsPick = true,
            isBeginnerFriendly = true,
            monthlyRank = 3,
            buyLinks = defaultLinks("The Quiet Compound")
        ),
        Book(
            id = "book_saffron_salt",
            title = "Saffron & Salt",
            subtitle = "A culinary romance in Jaipur's pink lanes",
            authorId = "auth_kavya",
            authorName = "Kavya Iyer",
            publisherId = "pub_penguin",
            publisherName = "Penguin Literary",
            year = 2025,
            pages = 288,
            estimatedHours = 4,
            language = "English",
            country = "India",
            ageRating = "13+",
            genre = "Romance",
            subGenre = "Contemporary Rom-Com",
            awards = listOf("Goodreads Choice Nominee"),
            overview = "Rival pastry chef Ananya and architectural restorer Kabir are forced to co-host a heritage haveli pop-up—where kitchen disasters spark unexpected warmth.",
            vibes = listOf(
                VibeSlice("Romance", 55, 0xFF9333EA),
                VibeSlice("Humor", 25, 0xFFD97706),
                VibeSlice("Drama", 20, 0xFFE05D44)
            ),
            ratingStats = RatingMeterStats(740, 960, 410, 45),
            coverRes = R.drawable.img_book_cover_2,
            accentColorHex = 0xFFC24D2C,
            isTalkOfTown = true,
            isBeginnerFriendly = true,
            monthlyRank = 5,
            buyLinks = defaultLinks("Saffron and Salt")
        ),
        Book(
            id = "book_tokyo_jazz_rain",
            title = "Rainy Vinyl in Shimokitazawa",
            subtitle = "Midnight confessions at a six-seat listening bar",
            authorId = "auth_haruki",
            authorName = "Kenji Takahashi",
            publisherId = "pub_penguin",
            publisherName = "Penguin Literary",
            year = 2023,
            pages = 310,
            estimatedHours = 5,
            language = "Japanese",
            country = "Japan",
            ageRating = "16+",
            genre = "Fiction",
            subGenre = "Surreal Literary",
            awards = listOf("Pulitzer Finalist", "Akutagawa Honor"),
            overview = "Every Thursday at 11 PM, a mysterious stranger drops off an unlabeled vinyl record at Café Nocturne. Listening to it lets patrons revisit a single unspoken conversation.",
            vibes = listOf(
                VibeSlice("Drama", 45, 0xFFE05D44),
                VibeSlice("Psychology", 35, 0xFF4F46E5),
                VibeSlice("Romance", 20, 0xFF9333EA)
            ),
            ratingStats = RatingMeterStats(1650, 740, 150, 32),
            coverRes = R.drawable.img_book_cover_1,
            accentColorHex = 0xFF312E81,
            isAppSelect = true,
            isEditorsPick = true,
            monthlyRank = 4,
            buyLinks = defaultLinks("Rainy Vinyl Shimokitazawa")
        ),
        Book(
            id = "book_raag_nagar",
            title = "राग नगर (Raag Nagar)",
            subtitle = "व्यंग्य और लोकतंत्र का जीवंत दर्पण",
            authorId = "auth_shrilal",
            authorName = "Raghavendra Joshi",
            publisherId = "pub_rajkamal",
            publisherName = "Rajkamal Heritage",
            year = 2021,
            pages = 372,
            estimatedHours = 7,
            language = "Hindi",
            country = "India",
            ageRating = "13+",
            genre = "Fiction",
            subGenre = "Satire & Classics",
            awards = listOf("Sahitya Akademi Award"),
            overview = "A sharp, witty portrait of village politics, idealist college graduates, and enduring human resilience in post-liberalization North India.",
            vibes = listOf(
                VibeSlice("Satire", 50, 0xFFD97706),
                VibeSlice("Drama", 30, 0xFFE05D44),
                VibeSlice("Psychology", 20, 0xFF4F46E5)
            ),
            ratingStats = RatingMeterStats(1420, 580, 95, 22),
            coverRes = R.drawable.img_book_cover_2,
            accentColorHex = 0xFF7C3AED,
            isAppSelect = true,
            monthlyRank = 6,
            buyLinks = defaultLinks("Raag Nagar Hindi")
        ),
        Book(
            id = "book_marathi_katha",
            title = "कोसलाचे दिवस (Days of the Cocoon)",
            subtitle = "तरुण मनाचा संवेदनशील प्रवास",
            authorId = "auth_meera",
            authorName = "Meera Deshmukh",
            publisherId = "pub_rajkamal",
            publisherName = "Rajkamal Heritage",
            year = 2022,
            pages = 240,
            estimatedHours = 4,
            language = "Marathi",
            country = "India",
            ageRating = "All Ages",
            genre = "Fiction",
            subGenre = "Young Adult Coming-of-Age",
            awards = listOf("Sahitya Yuva Puraskar"),
            overview = "A poignant Marathi coming-of-age story following a hostel student in Pune navigating friendship, first heartbreak, and finding his authentic literary voice.",
            vibes = listOf(
                VibeSlice("Drama", 50, 0xFFE05D44),
                VibeSlice("Nostalgia", 35, 0xFFD97706),
                VibeSlice("Romance", 15, 0xFF9333EA)
            ),
            ratingStats = RatingMeterStats(890, 520, 130, 19),
            coverRes = R.drawable.img_book_cover_1,
            accentColorHex = 0xFF8C4A27,
            isBeginnerFriendly = true,
            monthlyRank = 7,
            buyLinks = defaultLinks("Marathi Fiction")
        ),
        Book(
            id = "book_elderwood_1",
            title = "The Elderwood Crown (Book 1)",
            subtitle = "The Elderwood Quartet",
            authorId = "auth_clara",
            authorName = "Clara Vance",
            publisherId = "pub_penguin",
            publisherName = "Penguin Literary",
            year = 2024,
            pages = 412,
            estimatedHours = 7,
            language = "English",
            country = "USA",
            ageRating = "13+",
            genre = "Fiction",
            subGenre = "Young Adult Fantasy",
            awards = listOf("NYT Bestseller"),
            overview = "When the ancient library beneath Elderwood Academy begins erasing names from history, apprentice cartographer Lyra joins a secret society of ink-binders.",
            vibes = listOf(
                VibeSlice("Adventure", 45, 0xFF0D9488),
                VibeSlice("Mystery", 35, 0xFF4F46E5),
                VibeSlice("Romance", 20, 0xFF9333EA)
            ),
            ratingStats = RatingMeterStats(1520, 890, 260, 41),
            coverRes = R.drawable.img_book_cover_3,
            accentColorHex = 0xFF155E75,
            isSeriesOrManga = true,
            seriesLabel = "Elderwood Quartet #1",
            isBeginnerFriendly = true,
            monthlyRank = 8,
            buyLinks = defaultLinks("Elderwood Crown")
        ),
        Book(
            id = "book_aether_vol1",
            title = "Aether Chronicles Vol. 1",
            subtitle = "Rise of the Sky Cartographers",
            authorId = "auth_sora",
            authorName = "Sora Fujimoto",
            publisherId = "pub_shueisha",
            publisherName = "Kurogane Comics",
            year = 2023,
            pages = 198,
            estimatedHours = 2,
            language = "Japanese",
            country = "Japan",
            ageRating = "13+",
            genre = "Manga",
            subGenre = "Fantasy Manga",
            awards = listOf("Eisner Award Nominee"),
            overview = "Where the series began: young courier Kael discovers a living compass tattooed onto his wrist, opening the skies above floating islands.",
            vibes = listOf(
                VibeSlice("Adventure", 55, 0xFF0D9488),
                VibeSlice("Drama", 25, 0xFFE05D44),
                VibeSlice("Mystery", 20, 0xFF4F46E5)
            ),
            ratingStats = RatingMeterStats(2100, 790, 140, 28),
            coverRes = R.drawable.img_book_cover_1,
            accentColorHex = 0xFF0F766E,
            isAppSelect = true,
            isSeriesOrManga = true,
            seriesLabel = "Aether Chronicles #1",
            isBeginnerFriendly = true,
            monthlyRank = 9,
            buyLinks = defaultLinks("Aether Chronicles Vol 1")
        ),
        Book(
            id = "book_thinking_slow",
            title = "The Architecture of Calm",
            subtitle = "Why slow minds win complex decisions",
            authorId = "auth_arjun",
            authorName = "Arjun Malhotra",
            publisherId = "pub_harper",
            publisherName = "Harper Horizon",
            year = 2024,
            pages = 290,
            estimatedHours = 5,
            language = "English",
            country = "USA",
            ageRating = "All Ages",
            genre = "Non-Fiction",
            subGenre = "Cognitive Science",
            awards = listOf("Pulitzer Non-Fiction Honor"),
            overview = "An illuminating exploration of intuition, emotional regulation, and strategic patience across medicine, chess, and everyday leadership.",
            vibes = listOf(
                VibeSlice("Psychology", 55, 0xFF4F46E5),
                VibeSlice("Business", 25, 0xFFD97706),
                VibeSlice("Clarity", 20, 0xFF2F855A)
            ),
            ratingStats = RatingMeterStats(1180, 910, 200, 39),
            coverRes = R.drawable.img_book_cover_2,
            accentColorHex = 0xFF3F6212,
            isEditorsPick = true,
            isBeginnerFriendly = true,
            monthlyRank = 10,
            buyLinks = defaultLinks("The Architecture of Calm")
        )
    )

    val curatedCollections = listOf(
        CuratedCollectionSeed(
            id = "col_weekend_short",
            title = "Weekend Short Reads",
            curatorName = "nokhook Editorial",
            curatorUsername = "nokhook_editors",
            description = "Crisp, under-300-page gems you can finish over a rainy Sunday with masala chai.",
            topicTag = "Fiction",
            bookIds = listOf("book_quiet_compound", "book_saffron_salt", "book_marathi_katha", "book_aether_vol1")
                .map { if (it == "book_quiet_compound") "book_silent_algorithm" else it },
            followersCount = 14200
        ),
        CuratedCollectionSeed(
            id = "col_startup_must",
            title = "Startup & Focus Must-Reads",
            curatorName = "Arjun Malhotra",
            curatorUsername = "arjun_m",
            description = "Battle-tested books on deep focus, product craft, and calm execution.",
            topicTag = "Business",
            bookIds = listOf("book_silent_algorithm", "book_thinking_slow", "book_zero_quiet"),
            followersCount = 19800
        ),
        CuratedCollectionSeed(
            id = "col_monsoon_noir",
            title = "Atmospheric Monsoon Thrillers",
            curatorName = "Meera Deshmukh",
            curatorUsername = "meera_writes",
            description = "Misty hill stations, locked rooms, and psychological tension you won't put down.",
            topicTag = "Thriller",
            bookIds = listOf("book_shadows_shimla", "book_glass_monolith", "book_midnight_lighthouse"),
            followersCount = 11450
        ),
        CuratedCollectionSeed(
            id = "col_award_shelf",
            title = "Prize-Winning Indian & World Fiction",
            curatorName = "Priya Nair",
            curatorUsername = "priya_shelf",
            description = "Booker, Pulitzer, and Sahitya Akademi masterpieces curated for lifelong readers.",
            topicTag = "Award Winners",
            bookIds = listOf("book_midnight_lighthouse", "book_raag_nagar", "book_tokyo_jazz_rain"),
            followersCount = 22300
        )
    )

    val bookNews = listOf(
        BookNewsItem(
            id = "news_1",
            headline = "'The Midnight Lighthouse' Optioned for Limited Series",
            summary = "A six-part coastal drama adaptation begins filming in Scotland and Goa next spring with Clara Vance as executive producer.",
            source = "nokhook Literary Wire",
            timeAgo = "2h ago",
            topicTag = "Fiction",
            relatedBookId = "book_midnight_lighthouse"
        ),
        BookNewsItem(
            id = "news_2",
            headline = "Jaipur Literature & Indie Press Fest Announces 2027 Shortlist",
            summary = "Hindi, Marathi, and English translations take center stage with record readership among Gen-Z book clubs.",
            source = "South Asia Book Review",
            timeAgo = "6h ago",
            topicTag = "Award Winners",
            relatedBookId = "book_raag_nagar"
        ),
        BookNewsItem(
            id = "news_3",
            headline = "Sora Fujimoto Teases Double-Length Finale for Aether Vol. 9",
            summary = "Early preview sketches show the Eclipse Citadel arc expanding by 40 extra illustrated pages before global release.",
            source = "Kurogane Press Dispatch",
            timeAgo = "1d ago",
            topicTag = "Manga",
            relatedBookId = "book_aether_vol9"
        )
    )

    val bookSummaries = listOf(
        BookSummaryItem(
            id = "sum_1",
            bookId = "book_silent_algorithm",
            bookTitle = "The Quiet Compound",
            authorName = "Arjun Malhotra",
            durationLabel = "2:15 Animated Summary",
            formatLabel = "Video & Audio Brief",
            topicTag = "Business",
            keyTakeaway = "Protect the first 90 minutes of your morning from reactive feeds; tiny compounding rituals build extraordinary leverage.",
            youtubeUrl = "https://www.youtube.com/results?search_query=deep+work+habits+animated+book+summary",
            coverRes = R.drawable.img_book_cover_2
        ),
        BookSummaryItem(
            id = "sum_2",
            bookId = "book_midnight_lighthouse",
            bookTitle = "The Midnight Lighthouse",
            authorName = "Clara Vance",
            durationLabel = "2:40 Author Interview",
            formatLabel = "Studio Preview",
            topicTag = "Fiction",
            keyTakeaway = "Clara Vance shares how 1920s lighthouse keeper journals inspired Nora's haunting discovery.",
            youtubeUrl = "https://www.youtube.com/results?search_query=literary+fiction+author+interview+lighthouse",
            coverRes = R.drawable.img_book_cover_1
        ),
        BookSummaryItem(
            id = "sum_3",
            bookId = "book_thinking_slow",
            bookTitle = "The Architecture of Calm",
            authorName = "Arjun Malhotra",
            durationLabel = "1:58 Audio Sample",
            formatLabel = "Chapter 1 Preview",
            topicTag = "Self-Help",
            keyTakeaway = "Why high-stakes decision makers train emotional pause before pattern recognition.",
            youtubeUrl = "https://www.youtube.com/results?search_query=decision+making+psychology+book+summary",
            coverRes = R.drawable.img_book_cover_2
        )
    )

    val whatsNew = listOf(
        WhatsNewItem(
            id = "wn_1",
            versionTag = "v2.4 • NEW",
            title = "Vibe Match Engine 2.0",
            description = "Tap 'Find Similar Vibes →' on any book's Vibe Chart to discover books ranked by exact emotional & thematic percentage overlap.",
            dateLabel = "Nov 2026"
        ),
        WhatsNewItem(
            id = "wn_2",
            versionTag = "v2.3 • FEATURE",
            title = "4-Tier Rating Meter",
            description = "Say goodbye to vague 5-star averages. Rate books as Must Read, Worth Reading, Light Read, or Skip with instant community consensus.",
            dateLabel = "Oct 2026"
        ),
        WhatsNewItem(
            id = "wn_3",
            versionTag = "v2.2 • COMMUNITY",
            title = "Crowdsourced Missing Books",
            description = "Can't find an indie Hindi, Marathi, or regional gem? Submit it via My Contributions in the menu to help grow nokhook's library.",
            dateLabel = "Oct 2026"
        )
    )

    val seedReviews = listOf(
        SeedReview(
            id = "srev_1",
            bookId = "book_midnight_lighthouse",
            bookTitle = "The Midnight Lighthouse",
            userId = "seed_user_ananya",
            authorName = "Ananya Sen",
            authorUsername = "ananya_reads",
            timeAgo = "3h ago",
            ratingTier = RatingTier.MUST_READ,
            topicTag = "Fiction",
            body = "The atmosphere is unreal. Every page smells like salt spray and old parchment—I stayed up till 2 AM finishing the final logbook twist.",
            coverRes = R.drawable.img_book_cover_1
        ),
        SeedReview(
            id = "srev_2",
            bookId = "book_shadows_shimla",
            bookTitle = "Shadows of Shimla",
            userId = "seed_user_rohan",
            authorName = "Rohan Kapoor",
            authorUsername = "rohan_noir",
            timeAgo = "7h ago",
            ratingTier = RatingTier.MUST_READ,
            topicTag = "Thriller",
            body = "Inspector Deshmukh is my new favorite detective. Smart clues, zero filler, and crisp winter hill-station mood.",
            coverRes = R.drawable.img_book_cover_3
        ),
        SeedReview(
            id = "srev_3",
            bookId = "book_saffron_salt",
            bookTitle = "Saffron & Salt",
            userId = "seed_user_tara",
            authorName = "Tara Kulkarni",
            authorUsername = "tara_pages",
            timeAgo = "1d ago",
            ratingTier = RatingTier.LIGHT_READ,
            topicTag = "Romance",
            body = "Warm, comforting, and full of mouth-watering Jaipur dessert descriptions. Ideal weekend escapism.",
            coverRes = R.drawable.img_book_cover_2
        ),
        SeedReview(
            id = "srev_4",
            bookId = "book_silent_algorithm",
            bookTitle = "The Quiet Compound",
            userId = "seed_user_vikram",
            authorName = "Vikram Mehta",
            authorUsername = "vikram_builds",
            timeAgo = "2d ago",
            ratingTier = RatingTier.WORTH_READING,
            topicTag = "Business",
            body = "Actionable without being preachy. The Bengaluru founder chapter alone makes it worth keeping on your desk.",
            coverRes = R.drawable.img_book_cover_2
        )
    )

    val seedDiscussions = listOf(
        SeedDiscussion(
            id = "sdisc_1",
            userId = "seed_user_ananya",
            authorName = "Ananya Sen",
            authorUsername = "ananya_reads",
            topicTag = "Fiction",
            bookId = "book_midnight_lighthouse",
            title = "In The Midnight Lighthouse, do you read the 1924 entries as literal or memory?",
            body = "Chapter 14 left two completely valid interpretations on the table. Did Nora imagine the handwriting match, or was the lighthouse distorting time?",
            timeAgo = "4h ago",
            upvotes = 84,
            replies = listOf(
                SeedReply(
                    id = "srep_1",
                    authorName = "Rohan Kapoor",
                    authorUsername = "rohan_noir",
                    timeAgo = "3h ago",
                    body = "Notice the barometer pressure note in Chapter 3—it matches the exact storm date from 1924. Definitely uncanny realism!",
                    parentReplyId = ""
                ),
                SeedReply(
                    id = "srep_2",
                    authorName = "Ananya Sen",
                    authorUsername = "ananya_reads",
                    timeAgo = "2h ago",
                    body = "Good catch! I completely missed that barometer detail on my first pass.",
                    parentReplyId = "srep_1"
                )
            )
        ),
        SeedDiscussion(
            id = "sdisc_2",
            userId = "seed_user_vikram",
            authorName = "Vikram Mehta",
            authorUsername = "vikram_builds",
            topicTag = "Business",
            bookId = "book_silent_algorithm",
            title = "Best Hindi & English books for someone restarting a reading habit in 2026?",
            body = "Looking for books under 260 pages with high Vibe scores in Clarity or Adventure so a friend gets hooked fast.",
            timeAgo = "1d ago",
            upvotes = 63,
            replies = listOf(
                SeedReply(
                    id = "srep_3",
                    authorName = "Tara Kulkarni",
                    authorUsername = "tara_pages",
                    timeAgo = "20h ago",
                    body = "Start with Aether Chronicles Vol. 1 or Days of the Cocoon—both breeze past in one sitting.",
                    parentReplyId = ""
                )
            )
        )
    )

    val seedFollowingActivity = listOf(
        FollowedActivityItem(
            id = "fa_1",
            userName = "Ananya Sen",
            username = "ananya_reads",
            actionLabel = "marked as Read & rated",
            bookId = "book_midnight_lighthouse",
            bookTitle = "The Midnight Lighthouse",
            timeAgo = "2h ago",
            ratingTier = RatingTier.MUST_READ
        ),
        FollowedActivityItem(
            id = "fa_2",
            userName = "Rohan Kapoor",
            username = "rohan_noir",
            actionLabel = "marked Interested in",
            bookId = "book_glass_monolith",
            bookTitle = "The Glass Monolith",
            timeAgo = "5h ago",
            ratingTier = null
        ),
        FollowedActivityItem(
            id = "fa_3",
            userName = "Tara Kulkarni",
            username = "tara_pages",
            actionLabel = "added to Weekend Short Reads",
            bookId = "book_saffron_salt",
            bookTitle = "Saffron & Salt",
            timeAgo = "1d ago",
            ratingTier = RatingTier.LIGHT_READ
        )
    )

    val seedReaderProfiles = listOf(
        SeedReaderProfile(
            username = "ananya_reads",
            displayName = "Ananya Sen",
            bio = "Chasing coastal gothic mysteries, Booker shortlists & late-night chai reads.",
            followersCountLabel = "37.3K Followers",
            joinedLabel = "Joined 1 year ago",
            isVerified = true,
            accentColorHex = 0xFF4F46E5,
            reviewsCount = 148,
            publicCollectionsCount = 10,
            interestedBookIds = listOf("book_glass_monolith", "book_monsoon_malgudi", "book_aether_vol9"),
            socialUrlHint = "https://www.instagram.com/"
        ),
        SeedReaderProfile(
            username = "rohan_noir",
            displayName = "Rohan Kapoor",
            bio = "Locked-room thrillers, hill-station noir, and razor-sharp detective fiction.",
            followersCountLabel = "24.8K Followers",
            joinedLabel = "Joined 1 year ago",
            isVerified = true,
            accentColorHex = 0xFF1F3D36,
            reviewsCount = 96,
            publicCollectionsCount = 7,
            interestedBookIds = listOf("book_glass_monolith", "book_echoes_benares"),
            socialUrlHint = "https://x.com/"
        ),
        SeedReaderProfile(
            username = "tara_pages",
            displayName = "Tara Kulkarni",
            bio = "Cozy weekend rom-coms, Marathi classics & culinary storytelling.",
            followersCountLabel = "18.1K Followers",
            joinedLabel = "Joined 10 months ago",
            isVerified = true,
            accentColorHex = 0xFFC24D2C,
            reviewsCount = 64,
            publicCollectionsCount = 5,
            interestedBookIds = listOf("book_monsoon_malgudi", "book_zero_quiet"),
            socialUrlHint = "https://www.instagram.com/"
        ),
        SeedReaderProfile(
            username = "vikram_builds",
            displayName = "Vikram Mehta",
            bio = "Product builder annotating strategy, deep work, and founder memoirs.",
            followersCountLabel = "31.5K Followers",
            joinedLabel = "Joined 1 year ago",
            isVerified = true,
            accentColorHex = 0xFF2F6B55,
            reviewsCount = 82,
            publicCollectionsCount = 6,
            interestedBookIds = listOf("book_zero_quiet", "book_glass_monolith"),
            socialUrlHint = "https://x.com/"
        ),
        SeedReaderProfile(
            username = "arjun_m",
            displayName = "Arjun Malhotra",
            bio = "Author & behavioral economist curating focus and modern craft shelves.",
            followersCountLabel = "62.5K Followers",
            joinedLabel = "Joined 2 years ago",
            isVerified = true,
            accentColorHex = 0xFF2F6B55,
            reviewsCount = 41,
            publicCollectionsCount = 4,
            interestedBookIds = listOf("book_zero_quiet", "book_monsoon_malgudi"),
            socialUrlHint = "https://x.com/"
        ),
        SeedReaderProfile(
            username = "meera_writes",
            displayName = "Meera Deshmukh",
            bio = "Crime novelist writing misty hill-station suspense & psychological thrillers.",
            followersCountLabel = "48.2K Followers",
            joinedLabel = "Joined 2 years ago",
            isVerified = true,
            accentColorHex = 0xFF1F3A5F,
            reviewsCount = 38,
            publicCollectionsCount = 5,
            interestedBookIds = listOf("book_glass_monolith", "book_echoes_benares"),
            socialUrlHint = "https://www.instagram.com/"
        ),
        SeedReaderProfile(
            username = "priya_shelf",
            displayName = "Priya Nair",
            bio = "Tracking Booker, Pulitzer & Sahitya Akademi fiction across translations.",
            followersCountLabel = "29.4K Followers",
            joinedLabel = "Joined 1 year ago",
            isVerified = true,
            accentColorHex = 0xFF7C3AED,
            reviewsCount = 112,
            publicCollectionsCount = 9,
            interestedBookIds = listOf("book_echoes_benares", "book_monsoon_malgudi"),
            socialUrlHint = "https://x.com/"
        ),
        SeedReaderProfile(
            username = "aarav_reads",
            displayName = "Aarav Verma",
            bio = "Literary fiction critic & weekend manga collector.",
            followersCountLabel = "9.2K Followers",
            joinedLabel = "Joined 8 months ago",
            isVerified = true,
            accentColorHex = 0xFF0D9488,
            reviewsCount = 54,
            publicCollectionsCount = 4,
            interestedBookIds = listOf("book_aether_vol9", "book_glass_monolith"),
            socialUrlHint = "https://www.instagram.com/"
        )
    )

    fun getReaderProfile(usernameOrName: String): SeedReaderProfile {
        val clean = usernameOrName.trim().removePrefix("@")
        seedReaderProfiles.firstOrNull {
            it.username.equals(clean, ignoreCase = true) ||
                it.displayName.equals(clean, ignoreCase = true)
        }?.let { return it }

        val labelName = clean.replace('_', ' ').split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }
            .ifBlank { "nokhook Reader" }
        return SeedReaderProfile(
            username = clean.lowercase().replace(' ', '_').ifBlank { "reader" },
            displayName = labelName,
            bio = "Active reader and reviewer in the nokhook book community.",
            followersCountLabel = "1.4K Followers",
            joinedLabel = "Joined this year",
            isVerified = false,
            accentColorHex = 0xFF475569,
            reviewsCount = 6,
            publicCollectionsCount = 1,
            interestedBookIds = listOf("book_glass_monolith", "book_monsoon_malgudi")
        )
    }

    val allTopics = listOf("All Topics", "Fiction", "Thriller", "Business", "Self-Help", "Romance", "Manga", "Award Winners")

    val books: List<Book>
        get() = CatalogOverrideStore.getEffectiveBooks(seedBooks)

    fun getBookById(id: String): Book? = books.firstOrNull { it.id == id }
    fun getAuthorById(id: String): AuthorProfile? = authors.firstOrNull { it.id == id }
    fun getPublisherById(id: String): PublisherProfile? = publishers.firstOrNull { it.id == id }

    private val recommenderPool = listOf(
        RecommendationPerson(
            id = "rec_ananya",
            name = "Ananya Krishnan",
            profession = "Literary Critic & Book Club Host",
            imageRes = R.drawable.img_portrait_meera,
            quote = "A masterclass in pacing and atmospheric tension that stays with you for weeks.",
            username = "ananya_k"
        ),
        RecommendationPerson(
            id = "rec_rohan",
            name = "Rohan Kulkarni",
            profession = "Product Strategist & Essayist",
            imageRes = R.drawable.img_portrait_arjun,
            quote = "Sharp, humane, and effortlessly readable from the very first chapter.",
            username = "rohan_k"
        ),
        RecommendationPerson(
            id = "rec_devika",
            name = "Elena Vance-Reed",
            profession = "Senior Editor, Literary Review",
            imageRes = R.drawable.img_portrait_clara,
            quote = "Prose that balances quiet intimacy with unforgettable emotional stakes.",
            username = "clara_curates"
        ),
        RecommendationPerson(
            id = "rec_prof_joshi",
            name = "Prof. Vishwanath Mishra",
            profession = "Literature Professor & Jury Member",
            imageRes = R.drawable.img_portrait_raghavendra,
            quote = "Essential contemporary storytelling rooted in deep cultural craft.",
            username = "kabir_lit"
        ),
        RecommendationPerson(
            id = "rec_takeshi",
            name = "Haruto Sato",
            profession = "Manga Editor & Art Director",
            imageRes = R.drawable.img_portrait_kenji,
            quote = "Extraordinary visual world-building and memorable character arcs.",
            username = "manga_sensei"
        )
    )

    fun getRecommendationsForBook(book: Book): List<RecommendationPerson> {
        if (book.recommendations.isNotEmpty()) return book.recommendations
        return when {
            book.genre.equals("Manga", ignoreCase = true) || book.isSeriesOrManga -> listOf(
                recommenderPool[4],
                recommenderPool[0],
                recommenderPool[1]
            )
            book.genre.equals("Business", ignoreCase = true) || book.genre.equals("Self-Help", ignoreCase = true) -> listOf(
                recommenderPool[1],
                recommenderPool[0],
                recommenderPool[2]
            )
            book.language.equals("Hindi", ignoreCase = true) || book.language.equals("Marathi", ignoreCase = true) -> listOf(
                recommenderPool[3],
                recommenderPool[0],
                recommenderPool[2]
            )
            book.genre.equals("Thriller", ignoreCase = true) -> listOf(
                recommenderPool[0],
                recommenderPool[2],
                recommenderPool[4]
            )
            else -> listOf(
                recommenderPool[2],
                recommenderPool[3],
                recommenderPool[1]
            )
        }
    }

    /**
     * Computes cosine similarity between target book's Vibe vector and all other books.
     * Returns pairs of (Book, similarityPercentage 0..99).
     */
    fun findSimilarVibes(sourceBook: Book): List<Pair<Book, Int>> {
        val sourceMap = sourceBook.vibes.associate { it.name.lowercase() to it.percentage.toDouble() }
        val sourceNorm = kotlin.math.sqrt(sourceMap.values.sumOf { it * it }).coerceAtLeast(1.0)

        return books
            .filter { it.id != sourceBook.id }
            .map { candidate ->
                val candMap = candidate.vibes.associate { it.name.lowercase() to it.percentage.toDouble() }
                val candNorm = kotlin.math.sqrt(candMap.values.sumOf { it * it }).coerceAtLeast(1.0)
                val dot = sourceMap.entries.sumOf { (k, v) -> v * (candMap[k] ?: 0.0) }
                val cosine = (dot / (sourceNorm * candNorm)).coerceIn(0.0, 1.0)
                val genreBonus = if (candidate.genre == sourceBook.genre) 0.14 else 0.0
                val scorePct = (((cosine * 0.85) + genreBonus) * 100).toInt().coerceIn(28, 98)
                candidate to scorePct
            }
            .sortedByDescending { it.second }
    }
}

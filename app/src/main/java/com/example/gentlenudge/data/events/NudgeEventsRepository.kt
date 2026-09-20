package com.example.gentlenudge.data.events

import java.util.Calendar

/**
 * Robust, offline repository containing structured calendar events, festivals,
 * national holidays, cultural celebrations, religious observances, and awareness days.
 * 100% offline, predictable, and maintainable.
 */
object NudgeEventsRepository {

    // Cache indexed by formatted key "YYYY-MM-DD" and "MM-DD" for fast O(1) lookups
    private val fixedEventsByMonthDay = mutableMapOf<String, MutableList<NudgeCalendarEvent>>()
    private val yearSpecificEventsByKey = mutableMapOf<String, MutableList<NudgeCalendarEvent>>()

    init {
        loadEvents()
    }

    private fun registerFixedEvent(
        id: String,
        name: String,
        month: Int,
        day: Int,
        category: EventCategory,
        description: String,
        traditionOrRegion: String? = null,
        importance: EventImportance = EventImportance.STANDARD,
        dateBasis: DateBasis = DateBasis.FIXED_GREGORIAN,
        region: String? = traditionOrRegion,
        regionalVariationPossible: Boolean = false,
        calculationBasisOrSource: String? = null
    ) {
        val event = NudgeCalendarEvent(
            id = id,
            name = name,
            month = month,
            day = day,
            year = null,
            category = category,
            description = description,
            traditionOrRegion = traditionOrRegion,
            importance = importance,
            dateBasis = dateBasis,
            region = region,
            religionOrTradition = traditionOrRegion,
            regionalVariationPossible = regionalVariationPossible,
            calculationBasisOrSource = calculationBasisOrSource
        )
        val key = String.format("%02d-%02d", month, day)
        fixedEventsByMonthDay.getOrPut(key) { mutableListOf() }.add(event)
    }

    private fun registerYearEvent(
        id: String,
        name: String,
        year: Int,
        month: Int,
        day: Int,
        category: EventCategory,
        description: String,
        traditionOrRegion: String? = null,
        importance: EventImportance = EventImportance.STANDARD,
        dateBasis: DateBasis = DateBasis.HINDU_LUNAR,
        region: String? = traditionOrRegion,
        regionalVariationPossible: Boolean = false,
        calculationBasisOrSource: String? = null
    ) {
        val event = NudgeCalendarEvent(
            id = id,
            name = name,
            month = month,
            day = day,
            year = year,
            category = category,
            description = description,
            traditionOrRegion = traditionOrRegion,
            importance = importance,
            dateBasis = dateBasis,
            region = region,
            religionOrTradition = traditionOrRegion,
            regionalVariationPossible = regionalVariationPossible,
            calculationBasisOrSource = calculationBasisOrSource
        )
        val key = String.format("%04d-%02d-%02d", year, month, day)
        yearSpecificEventsByKey.getOrPut(key) { mutableListOf() }.add(event)
    }

    /**
     * Retrieves all events occurring on the specified date.
     * @param year The 4-digit calendar year (e.g. 2026)
     * @param month The 1-based month (1 = January, 12 = December)
     * @param day The day of the month (1-31)
     */
    fun getEventsForDate(year: Int, month: Int, day: Int): List<NudgeCalendarEvent> {
        val result = mutableListOf<NudgeCalendarEvent>()

        // 1. Fixed recurring events
        val fixedKey = String.format("%02d-%02d", month, day)
        fixedEventsByMonthDay[fixedKey]?.let { result.addAll(it) }

        // 2. Year-specific lunisolar/movable events
        val yearKey = String.format("%04d-%02d-%02d", year, month, day)
        yearSpecificEventsByKey[yearKey]?.let { result.addAll(it) }

        return result
    }

    /**
     * Retrieves all events occurring on the date specified by the Calendar instance.
     */
    fun getEventsForCalendar(calendar: Calendar): List<NudgeCalendarEvent> {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // Convert 0-based to 1-based
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return getEventsForDate(year, month, day)
    }

    /**
     * Fast check if a date contains one or more events.
     */
    fun hasEventsOnDate(year: Int, month: Int, day: Int): Boolean {
        val fixedKey = String.format("%02d-%02d", month, day)
        if (fixedEventsByMonthDay.containsKey(fixedKey)) return true

        val yearKey = String.format("%04d-%02d-%02d", year, month, day)
        return yearSpecificEventsByKey.containsKey(yearKey)
    }

    /**
     * Retrieves only Hindu festival and observance events for the specified date.
     */
    fun getHinduEventsForDate(year: Int, month: Int, day: Int): List<NudgeCalendarEvent> {
        return getEventsForDate(year, month, day).filter { it.category == EventCategory.HINDU_FESTIVAL }
    }

    /**
     * Retrieves international, world observances, and cultural/religious events for the date.
     */
    fun getInternationalAndWorldEventsForDate(year: Int, month: Int, day: Int): List<NudgeCalendarEvent> {
        return getEventsForDate(year, month, day).filter { it.category != EventCategory.HINDU_FESTIVAL }
    }

    /**
     * Fast check if date contains a Hindu festival.
     */
    fun hasHinduEventOnDate(year: Int, month: Int, day: Int): Boolean {
        return getHinduEventsForDate(year, month, day).isNotEmpty()
    }

    /**
     * Fast check if date contains an international or world observance.
     */
    fun hasInternationalEventOnDate(year: Int, month: Int, day: Int): Boolean {
        return getInternationalAndWorldEventsForDate(year, month, day).isNotEmpty()
    }

    private fun loadEvents() {
        // =========================================================================
        // 1. FIXED ANNUAL OBSERVANCES & NATIONAL DAYS (All Years)
        // =========================================================================

        // --- JANUARY ---
        registerFixedEvent(
            "fix_jan_01", "New Year's Day", 1, 1,
            EventCategory.PUBLIC_HOLIDAY,
            "Welcoming the start of the Gregorian new year with hope, renewal, and fresh beginnings."
        )
        registerFixedEvent(
            "fix_jan_01_fam", "Global Family Day", 1, 1,
            EventCategory.INTERNATIONAL_DAY,
            "A worldwide celebration of peace, harmony, and the shared human family."
        )
        registerFixedEvent(
            "fix_jan_03_savitribai", "Savitribai Phule Jayanti", 1, 3,
            EventCategory.NATIONAL_DAY,
            "Honoring pioneering social reformer and educator who champion women's education and equality in India.",
            traditionOrRegion = "Pan-India"
        )
        registerFixedEvent(
            "fix_jan_04", "World Braille Day", 1, 4,
            EventCategory.AWARENESS_DAY,
            "Celebrating the importance of Braille as a medium of communication for blind and partially sighted people."
        )
        registerFixedEvent(
            "fix_jan_12", "National Youth Day", 1, 12,
            EventCategory.NATIONAL_DAY,
            "Commemorating the birthday of Swami Vivekananda and inspiring youth toward strength and purpose."
        )
        registerFixedEvent(
            "fix_jan_13_lohri", "Lohri", 1, 13,
            EventCategory.CULTURAL_EVENT,
            "Folk festival celebrating the winter harvest, bonfire rituals, and thanksgiving in Northern India.",
            traditionOrRegion = "North India"
        )
        registerFixedEvent(
            "fix_jan_14_sankranti", "Makar Sankranti / Pongal / Magh Bihu", 1, 14,
            EventCategory.HINDU_FESTIVAL,
            "Harvest festival celebrating the sun's transition into Capricorn (Makara): Sankranti, Pongal (TN), and Magh Bihu (Assam).",
            traditionOrRegion = "Pan-India"
        )
        registerFixedEvent(
            "fix_jan_15_kanuma", "Mattu Pongal / Kanuma Panduga", 1, 15,
            EventCategory.HINDU_FESTIVAL,
            "Harvest celebration honoring farm cattle, bulls, and agriculture with thanksgiving feasts and rituals.",
            traditionOrRegion = "South India"
        )
        registerFixedEvent(
            "fix_jan_15", "Indian Army Day", 1, 15,
            EventCategory.NATIONAL_DAY,
            "Honoring the valor, sacrifice, and dedication of the soldiers of the Indian Army."
        )
        registerFixedEvent(
            "fix_jan_23_netaji", "Netaji Subhas Chandra Bose Jayanti (Parakram Diwas)", 1, 23,
            EventCategory.NATIONAL_DAY,
            "National day of valor commemorating the birth anniversary of Netaji Subhas Chandra Bose.",
            traditionOrRegion = "Pan-India"
        )
        registerFixedEvent(
            "fix_jan_24", "National Girl Child Day", 1, 24,
            EventCategory.NATIONAL_DAY,
            "Promoting empowerment, education, and equal rights for every girl child in India."
        )
        registerFixedEvent(
            "fix_jan_24_edu", "International Day of Education", 1, 24,
            EventCategory.INTERNATIONAL_DAY,
            "Celebrating education's essential role in peace, equity, and human development."
        )
        registerFixedEvent(
            "fix_jan_25_voters", "National Voters' Day (India)", 1, 25,
            EventCategory.NATIONAL_DAY,
            "Encouraging civic participation and voter awareness to strengthen democratic processes.",
            traditionOrRegion = "Pan-India"
        )
        registerFixedEvent(
            "fix_jan_25_tourism", "National Tourism Day (India)", 1, 25,
            EventCategory.CULTURAL_EVENT,
            "Appreciating India's rich cultural diversity, historic landscapes, and heritage tourism.",
            traditionOrRegion = "Pan-India"
        )
        registerFixedEvent(
            "fix_jan_26", "Republic Day of India", 1, 26,
            EventCategory.NATIONAL_DAY,
            "National holiday celebrating the date on which the Constitution of India came into effect in 1950."
        )
        registerFixedEvent(
            "fix_jan_30", "Martyrs' Day (Shaheed Diwas)", 1, 30,
            EventCategory.NATIONAL_DAY,
            "Honoring Mahatma Gandhi and the brave freedom fighters who laid down their lives for the nation."
        )

        // --- FEBRUARY ---
        registerFixedEvent(
            "fix_feb_04", "World Cancer Day", 2, 4,
            EventCategory.AWARENESS_DAY,
            "Global initiative raising awareness, improving education, and catalyzing personal and collective action."
        )
        registerFixedEvent(
            "fix_feb_13", "World Radio Day", 2, 13,
            EventCategory.INTERNATIONAL_DAY,
            "Recognizing radio as a powerful medium for celebrating humanity and democratic discourse."
        )
        registerFixedEvent(
            "fix_feb_13_women", "National Women's Day (India)", 2, 13,
            EventCategory.NATIONAL_DAY,
            "Commemorating the birth anniversary of Sarojini Naidu, the Nightingale of India."
        )
        registerFixedEvent(
            "fix_feb_19_shivaji", "Chhatrapati Shivaji Maharaj Jayanti", 2, 19,
            EventCategory.NATIONAL_DAY,
            "Birth anniversary of the legendary Maratha ruler and visionary pioneer Chhatrapati Shivaji Maharaj.",
            traditionOrRegion = "Maharashtra / Pan-India"
        )
        registerFixedEvent(
            "fix_feb_20", "World Day of Social Justice", 2, 20,
            EventCategory.INTERNATIONAL_DAY,
            "Promoting social justice, decent work, gender equality, and fundamental human rights."
        )
        registerFixedEvent(
            "fix_feb_21", "International Mother Language Day", 2, 21,
            EventCategory.INTERNATIONAL_DAY,
            "Promoting linguistic and cultural diversity, multilingualism, and mother tongue preservation."
        )
        registerFixedEvent(
            "fix_feb_28", "National Science Day", 2, 28,
            EventCategory.NATIONAL_DAY,
            "Commemorating the discovery of the Raman Effect by Sir C.V. Raman in 1928."
        )

        // --- MARCH ---
        registerFixedEvent(
            "fix_mar_03", "World Wildlife Day", 3, 3,
            EventCategory.AWARENESS_DAY,
            "Celebrating the extraordinary beauty and diversity of wild flora and fauna."
        )
        registerFixedEvent(
            "fix_mar_04_safety", "National Safety Day (India)", 3, 4,
            EventCategory.AWARENESS_DAY,
            "Promoting health, safety culture, and environmental protection across Indian industries and workplaces.",
            traditionOrRegion = "Pan-India"
        )
        registerFixedEvent(
            "fix_mar_08", "International Women's Day", 3, 8,
            EventCategory.INTERNATIONAL_DAY,
            "Global day celebrating the social, economic, cultural, and political achievements of women."
        )
        registerFixedEvent(
            "fix_mar_15", "World Consumer Rights Day", 3, 15,
            EventCategory.AWARENESS_DAY,
            "Advocating for consumer protection, safety, and fair global market practices."
        )
        registerFixedEvent(
            "fix_mar_20", "International Day of Happiness", 3, 20,
            EventCategory.INTERNATIONAL_DAY,
            "Recognizing happiness and well-being as universal goals and aspirations in human lives."
        )
        registerFixedEvent(
            "fix_mar_20_sparrow", "World Sparrow Day", 3, 20,
            EventCategory.AWARENESS_DAY,
            "Raising awareness about the conservation of house sparrows and urban biodiversity."
        )
        registerFixedEvent(
            "fix_mar_21", "International Day of Forests", 3, 21,
            EventCategory.AWARENESS_DAY,
            "Celebrating and raising awareness of the importance of all types of forests and trees."
        )
        registerFixedEvent(
            "fix_mar_21_poetry", "World Poetry Day", 3, 21,
            EventCategory.CULTURAL_EVENT,
            "Honoring poets, reviving oral traditions, and celebrating linguistic rhythm and expression."
        )
        registerFixedEvent(
            "fix_mar_21_navroz", "Jamshedi Navroz", 3, 21,
            EventCategory.CULTURAL_EVENT,
            "Spring equinox celebration observed with joyous customs by the Indian Parsi and Irani community.",
            traditionOrRegion = "Parsi / Zoroastrian"
        )
        registerFixedEvent(
            "fix_mar_22", "World Water Day", 3, 22,
            EventCategory.AWARENESS_DAY,
            "Focusing on the importance of freshwater and advocating for sustainable freshwater management."
        )
        registerFixedEvent(
            "fix_mar_23", "Shaheed Diwas (Bhagat Singh, Sukhdev, Rajguru)", 3, 23,
            EventCategory.NATIONAL_DAY,
            "Paying tribute to the extraordinary courage and sacrifice of revolutionary freedom fighters."
        )
        registerFixedEvent(
            "fix_mar_24", "World Tuberculosis Day", 3, 24,
            EventCategory.AWARENESS_DAY,
            "Building public awareness about the health, social, and economic impact of tuberculosis."
        )

        // --- APRIL ---
        registerFixedEvent(
            "fix_apr_07", "World Health Day", 4, 7,
            EventCategory.AWARENESS_DAY,
            "Marking the founding of the WHO and championing universal health and wellness."
        )
        registerFixedEvent(
            "fix_apr_14", "Dr. B.R. Ambedkar Jayanti", 4, 14,
            EventCategory.NATIONAL_DAY,
            "Honoring the principal architect of the Indian Constitution, social reformer, and jurist.",
            traditionOrRegion = "Pan-India"
        )
        registerFixedEvent(
            "fix_apr_14_solar_ny", "Puthandu / Vishu / Bohag Bihu", 4, 14,
            EventCategory.CULTURAL_EVENT,
            "Solar New Year celebration marking harvest and renewal: Puthandu (Tamil Nadu), Vishu (Kerala), and Rongali Bihu (Assam).",
            traditionOrRegion = "South / East India"
        )
        registerFixedEvent(
            "fix_apr_15_pohela_boishakh", "Pohela Boishakh (Bengali New Year)", 4, 15,
            EventCategory.CULTURAL_EVENT,
            "First day of the Bengali calendar (Noboborsho) celebrated with cultural songs, sweets, and new ledger books (Haal Khata).",
            traditionOrRegion = "West Bengal / Tripura"
        )
        registerFixedEvent(
            "fix_apr_18", "World Heritage Day", 4, 18,
            EventCategory.CULTURAL_EVENT,
            "Promoting cultural heritage preservation and celebrating historic monuments and landmarks."
        )
        registerFixedEvent(
            "fix_apr_21", "National Civil Services Day", 4, 21,
            EventCategory.NATIONAL_DAY,
            "Appreciating civil servants dedicated to serving citizens and public governance."
        )
        registerFixedEvent(
            "fix_apr_22", "Earth Day", 4, 22,
            EventCategory.INTERNATIONAL_DAY,
            "Global environmental movement demonstrating support for environmental protection and conservation."
        )
        registerFixedEvent(
            "fix_apr_23", "World Book and Copyright Day", 4, 23,
            EventCategory.CULTURAL_EVENT,
            "Celebrating the joy of reading, authors, books, and intellectual property."
        )
        registerFixedEvent(
            "fix_apr_24_panchayati_raj", "National Panchayati Raj Day", 4, 24,
            EventCategory.NATIONAL_DAY,
            "Commemorating the institutionalization of Panchayati Raj through the 73rd Constitutional Amendment Act, 1992.",
            traditionOrRegion = "Pan-India"
        )
        registerFixedEvent(
            "fix_apr_25", "World Malaria Day", 4, 25,
            EventCategory.AWARENESS_DAY,
            "Recognizing global efforts to prevent, treat, and eradicate malaria."
        )

        // --- MAY ---
        registerFixedEvent(
            "fix_may_01", "International Workers' Day (Labour Day)", 5, 1,
            EventCategory.PUBLIC_HOLIDAY,
            "Celebrating the labor movement, workers' rights, and economic contributions of laborers worldwide."
        )
        registerFixedEvent(
            "fix_may_03", "World Press Freedom Day", 5, 3,
            EventCategory.INTERNATIONAL_DAY,
            "Defending the fundamental principles of free press and honoring journalists."
        )
        registerFixedEvent(
            "fix_may_07_tagore", "Rabindranath Tagore Jayanti (Rabindra Jayanti)", 5, 7,
            EventCategory.CULTURAL_EVENT,
            "Honoring Asia's first Nobel laureate, polymath, poet, and composer of India's national anthem.",
            traditionOrRegion = "West Bengal / Pan-India"
        )
        registerFixedEvent(
            "fix_may_08", "World Red Cross and Red Crescent Day", 5, 8,
            EventCategory.INTERNATIONAL_DAY,
            "Celebrating humanitarian principles and emergency aid volunteers across the globe."
        )
        registerFixedEvent(
            "fix_may_11", "National Technology Day (India)", 5, 11,
            EventCategory.NATIONAL_DAY,
            "Commemorating India's scientific breakthroughs and technological achievements."
        )
        registerFixedEvent(
            "fix_may_15", "International Day of Families", 5, 15,
            EventCategory.INTERNATIONAL_DAY,
            "Celebrating the importance of family bonds, support systems, and social cohesion."
        )
        registerFixedEvent(
            "fix_may_21", "National Anti-Terrorism Day", 5, 21,
            EventCategory.NATIONAL_DAY,
            "Promoting national unity, peace, and rejecting violence and extremism."
        )
        registerFixedEvent(
            "fix_may_22", "International Day for Biological Diversity", 5, 22,
            EventCategory.AWARENESS_DAY,
            "Promoting understanding and awareness of biodiversity issues and ecosystems."
        )
        registerFixedEvent(
            "fix_may_31", "World No Tobacco Day", 5, 31,
            EventCategory.AWARENESS_DAY,
            "Highlighting health risks associated with tobacco use and advocating for smoke-free living."
        )

        // --- JUNE ---
        registerFixedEvent(
            "fix_jun_05", "World Environment Day", 6, 5,
            EventCategory.INTERNATIONAL_DAY,
            "The UN's flagship day for encouraging global awareness and action for the protection of our environment."
        )
        registerFixedEvent(
            "fix_jun_07", "World Food Safety Day", 6, 7,
            EventCategory.AWARENESS_DAY,
            "Drawing attention and mobilizing action to prevent, detect, and manage foodborne risks."
        )
        registerFixedEvent(
            "fix_jun_08", "World Oceans Day", 6, 8,
            EventCategory.AWARENESS_DAY,
            "Celebrating the world's shared ocean and mobilizing collective action for marine conservation."
        )
        registerFixedEvent(
            "fix_jun_12", "World Day Against Child Labour", 6, 12,
            EventCategory.AWARENESS_DAY,
            "Focusing global attention on the eradication of child labor worldwide."
        )
        registerFixedEvent(
            "fix_jun_14", "World Blood Donor Day", 6, 14,
            EventCategory.AWARENESS_DAY,
            "Thanking voluntary, unpaid blood donors for their life-saving gifts of blood."
        )
        registerFixedEvent(
            "fix_jun_20", "World Refugee Day", 6, 20,
            EventCategory.INTERNATIONAL_DAY,
            "Honoring the courage, strength, and resilience of refugees around the world."
        )
        registerFixedEvent(
            "fix_jun_21", "International Day of Yoga", 6, 21,
            EventCategory.INTERNATIONAL_DAY,
            "Celebrating the ancient holistic practice of Yoga for physical, mental, and spiritual harmony."
        )
        registerFixedEvent(
            "fix_jun_21_music", "World Music Day (Fête de la Musique)", 6, 21,
            EventCategory.CULTURAL_EVENT,
            "Celebrating the universal language of music with live performances and artistic expression."
        )
        registerFixedEvent(
            "fix_jun_23", "International Olympic Day", 6, 23,
            EventCategory.INTERNATIONAL_DAY,
            "Promoting sports, healthy living, and the Olympic values of excellence, friendship, and respect."
        )
        registerFixedEvent(
            "fix_jun_29", "National Statistics Day", 6, 29,
            EventCategory.NATIONAL_DAY,
            "Commemorating the birth anniversary of Prof. P.C. Mahalanobis and the role of statistics in planning."
        )

        // --- JULY ---
        registerFixedEvent(
            "fix_jul_01", "National Doctor's Day", 7, 1,
            EventCategory.NATIONAL_DAY,
            "Honoring medical professionals for their selfless service and devotion to saving lives."
        )
        registerFixedEvent(
            "fix_jul_11", "World Population Day", 7, 11,
            EventCategory.AWARENESS_DAY,
            "Focusing attention on the urgency and importance of population issues and reproductive health."
        )
        registerFixedEvent(
            "fix_jul_15", "World Youth Skills Day", 7, 15,
            EventCategory.INTERNATIONAL_DAY,
            "Celebrating the importance of equipping young people with skills for employment and entrepreneurship."
        )
        registerFixedEvent(
            "fix_jul_26", "Kargil Vijay Diwas", 7, 26,
            EventCategory.NATIONAL_DAY,
            "Commemorating the success of Operation Vijay and honoring the brave armed forces martyrs."
        )
        registerFixedEvent(
            "fix_jul_28", "World Nature Conservation Day", 7, 28,
            EventCategory.AWARENESS_DAY,
            "Acknowledging that a healthy environment is the foundation for a stable and productive society."
        )
        registerFixedEvent(
            "fix_jul_29", "International Tiger Day", 7, 29,
            EventCategory.AWARENESS_DAY,
            "Raising awareness for tiger conservation, protecting natural habitats, and preventing poaching."
        )

        // --- AUGUST ---
        registerFixedEvent(
            "fix_aug_07", "National Handloom Day", 8, 7,
            EventCategory.NATIONAL_DAY,
            "Honoring the handloom weaving community and preserving traditional Indian textile heritage."
        )
        registerFixedEvent(
            "fix_aug_07_javelin", "National Javelin Day", 8, 7,
            EventCategory.NATIONAL_DAY,
            "Commemorating Neeraj Chopra's historic Olympic gold medal in javelin throw."
        )
        registerFixedEvent(
            "fix_aug_09", "Quit India Movement Day", 8, 9,
            EventCategory.NATIONAL_DAY,
            "Commemorating the launch of the historic Quit India movement led by Mahatma Gandhi in 1942."
        )
        registerFixedEvent(
            "fix_aug_12", "International Youth Day", 8, 12,
            EventCategory.INTERNATIONAL_DAY,
            "Drawing attention to youth issues and celebrating their potential as partners in global society."
        )
        registerFixedEvent(
            "fix_aug_15", "Independence Day of India", 8, 15,
            EventCategory.NATIONAL_DAY,
            "National holiday celebrating India's independence from British colonial rule on August 15, 1947."
        )
        registerFixedEvent(
            "fix_aug_19", "World Photography Day", 8, 19,
            EventCategory.CULTURAL_EVENT,
            "Celebrating the art, craft, science, and history of photography worldwide."
        )
        registerFixedEvent(
            "fix_aug_19_human", "World Humanitarian Day", 8, 19,
            EventCategory.INTERNATIONAL_DAY,
            "Honoring humanitarian workers who face danger and adversity to help people in crises."
        )
        registerFixedEvent(
            "fix_aug_20", "Sadbhavana Diwas (Harmony Day)", 8, 20,
            EventCategory.NATIONAL_DAY,
            "Promoting national integration, communal harmony, and peace among all religions and communities."
        )
        registerFixedEvent(
            "fix_aug_23_space", "National Space Day (India)", 8, 23,
            EventCategory.NATIONAL_DAY,
            "Commemorating India's historic landing of Chandrayaan-3 on the Moon and celebrating space achievements.",
            traditionOrRegion = "Pan-India"
        )
        registerFixedEvent(
            "fix_aug_26", "Women's Equality Day", 8, 26,
            EventCategory.INTERNATIONAL_DAY,
            "Commemorating the passage of women's voting rights and championing gender equality."
        )
        registerFixedEvent(
            "fix_aug_29", "National Sports Day", 8, 29,
            EventCategory.NATIONAL_DAY,
            "Commemorating the birth anniversary of hockey wizard Major Dhyan Chand."
        )

        // --- SEPTEMBER ---
        registerFixedEvent(
            "fix_sep_05", "Teachers' Day (India)", 9, 5,
            EventCategory.NATIONAL_DAY,
            "Honoring teachers and mentors on the birth anniversary of Dr. Sarvepalli Radhakrishnan."
        )
        registerFixedEvent(
            "fix_sep_05_charity", "International Day of Charity", 9, 5,
            EventCategory.INTERNATIONAL_DAY,
            "Commemorating the anniversary of Mother Teresa's passing and promoting charitable acts."
        )
        registerFixedEvent(
            "fix_sep_08", "International Literacy Day", 9, 8,
            EventCategory.INTERNATIONAL_DAY,
            "Highlighting literacy as a matter of dignity and human rights across communities."
        )
        registerFixedEvent(
            "fix_sep_14", "Hindi Diwas", 9, 14,
            EventCategory.NATIONAL_DAY,
            "Celebrating the adoption of Hindi as an official language of the Union of India."
        )
        registerFixedEvent(
            "fix_sep_15", "National Engineers' Day", 9, 15,
            EventCategory.NATIONAL_DAY,
            "Tribute to the greatest Indian engineer, Sir M. Visvesvaraya, on his birth anniversary."
        )
        registerFixedEvent(
            "fix_sep_16", "World Ozone Day", 9, 16,
            EventCategory.AWARENESS_DAY,
            "Commemorating the signing of the Montreal Protocol for the preservation of the ozone layer."
        )
        registerFixedEvent(
            "fix_sep_17_vishwakarma", "Vishwakarma Puja", 9, 17,
            EventCategory.HINDU_FESTIVAL,
            "Honoring Lord Vishwakarma, the divine architect and craftsman, celebrated by engineers, artisans, and creators.",
            traditionOrRegion = "Pan-India"
        )
        registerFixedEvent(
            "fix_sep_21", "International Day of Peace", 9, 21,
            EventCategory.INTERNATIONAL_DAY,
            "Devoted to strengthening the ideals of peace, both within and among all nations and peoples."
        )
        registerFixedEvent(
            "fix_sep_27", "World Tourism Day", 9, 27,
            EventCategory.CULTURAL_EVENT,
            "Fostering awareness of tourism's social, cultural, political, and economic value."
        )
        registerFixedEvent(
            "fix_sep_29", "World Heart Day", 9, 29,
            EventCategory.AWARENESS_DAY,
            "Informing people around the world about cardiovascular disease prevention and heart health."
        )

        // --- OCTOBER ---
        registerFixedEvent(
            "fix_oct_01", "International Day of Older Persons", 10, 1,
            EventCategory.INTERNATIONAL_DAY,
            "Honoring senior citizens and addressing opportunities and challenges of population ageing."
        )
        registerFixedEvent(
            "fix_oct_02", "Mahatma Gandhi Jayanti", 10, 2,
            EventCategory.NATIONAL_DAY,
            "National holiday commemorating the birth of the Father of the Nation, Mohandas Karamchand Gandhi."
        )
        registerFixedEvent(
            "fix_oct_02_peace", "International Day of Non-Violence", 10, 2,
            EventCategory.INTERNATIONAL_DAY,
            "UN observance disseminating the message of non-violence, peace, and tolerance."
        )
        registerFixedEvent(
            "fix_oct_02_shastri", "Lal Bahadur Shastri Jayanti", 10, 2,
            EventCategory.NATIONAL_DAY,
            "Remembering the second Prime Minister of India who gave the slogan 'Jai Jawan Jai Kisan'."
        )
        registerFixedEvent(
            "fix_oct_04", "World Animal Welfare Day", 10, 4,
            EventCategory.AWARENESS_DAY,
            "Raising the status of animals in order to improve welfare standards around the globe."
        )
        registerFixedEvent(
            "fix_oct_05", "World Teachers' Day", 10, 5,
            EventCategory.INTERNATIONAL_DAY,
            "Celebrating educators worldwide and their transformative impact on students and communities."
        )
        registerFixedEvent(
            "fix_oct_08", "Indian Air Force Day", 10, 8,
            EventCategory.NATIONAL_DAY,
            "Celebrating the foundation and gallant history of the Indian Air Force."
        )
        registerFixedEvent(
            "fix_oct_09", "World Post Day", 10, 9,
            EventCategory.INTERNATIONAL_DAY,
            "Recognizing the role of postal services in everyday lives of people and businesses."
        )
        registerFixedEvent(
            "fix_oct_10", "World Mental Health Day", 10, 10,
            EventCategory.AWARENESS_DAY,
            "Raising awareness of mental health issues and mobilizing efforts in support of mental wellness."
        )
        registerFixedEvent(
            "fix_oct_11", "International Day of the Girl Child", 10, 11,
            EventCategory.INTERNATIONAL_DAY,
            "Advocating for girls' rights and highlighting unique challenges girls face worldwide."
        )
        registerFixedEvent(
            "fix_oct_15", "World Students' Day", 10, 15,
            EventCategory.NATIONAL_DAY,
            "Marking the birth anniversary of Dr. A.P.J. Abdul Kalam, dedicated teacher and Missile Man of India."
        )
        registerFixedEvent(
            "fix_oct_16", "World Food Day", 10, 16,
            EventCategory.AWARENESS_DAY,
            "Promoting global awareness and action for those who suffer from hunger and nutritious food security."
        )
        registerFixedEvent(
            "fix_oct_18_kati_bihu", "Kati Bihu (Kongali Bihu)", 10, 18,
            EventCategory.CULTURAL_EVENT,
            "Solemn Assamese agricultural observance lighting earthen lamps (Saki) in paddy fields to pray for a good harvest.",
            traditionOrRegion = "Assam / Northeast India"
        )
        registerFixedEvent(
            "fix_oct_24", "United Nations Day", 10, 24,
            EventCategory.INTERNATIONAL_DAY,
            "Marking the anniversary of the entry into force of the UN Charter in 1945."
        )
        registerFixedEvent(
            "fix_oct_31", "National Unity Day (Rashtriya Ekta Diwas)", 10, 31,
            EventCategory.NATIONAL_DAY,
            "Commemorating the birth anniversary of Sardar Vallabhbhai Patel, the Iron Man of India."
        )

        // --- NOVEMBER ---
        registerFixedEvent(
            "fix_nov_01_statehood", "State Formation Day (Karnataka Rajyotsava / Kerala Piravi)", 11, 1,
            EventCategory.CULTURAL_EVENT,
            "Celebrating state formation day across Karnataka, Kerala, Andhra Pradesh, Madhya Pradesh, Chhattisgarh, and Haryana.",
            traditionOrRegion = "South / Central India"
        )
        registerFixedEvent(
            "fix_nov_05", "World Tsunami Awareness Day", 11, 5,
            EventCategory.AWARENESS_DAY,
            "Promoting disaster risk reduction and community tsunami preparedness."
        )
        registerFixedEvent(
            "fix_nov_07", "National Cancer Awareness Day", 11, 7,
            EventCategory.AWARENESS_DAY,
            "Highlighting early detection, prevention, and compassionate care for cancer patients."
        )
        registerFixedEvent(
            "fix_nov_11", "National Education Day", 11, 11,
            EventCategory.NATIONAL_DAY,
            "Commemorating the birth anniversary of Maulana Abul Kalam Azad, India's first Education Minister."
        )
        registerFixedEvent(
            "fix_nov_13", "World Kindness Day", 11, 13,
            EventCategory.INTERNATIONAL_DAY,
            "Encouraging goodwill, empathy, and small, heartfelt acts of kindness."
        )
        registerFixedEvent(
            "fix_nov_14", "Children's Day (Bal Diwas)", 11, 14,
            EventCategory.NATIONAL_DAY,
            "Celebrating children on the birth anniversary of Pandit Jawaharlal Nehru (Chacha Nehru)."
        )
        registerFixedEvent(
            "fix_nov_14_diab", "World Diabetes Day", 11, 14,
            EventCategory.AWARENESS_DAY,
            "Raising awareness about diabetes care, lifestyle management, and prevention."
        )
        registerFixedEvent(
            "fix_nov_19", "National Integration Day", 11, 19,
            EventCategory.NATIONAL_DAY,
            "Marking the birth anniversary of Indira Gandhi and fostering national harmony and solidarity."
        )
        registerFixedEvent(
            "fix_nov_20", "World Children's Day", 11, 20,
            EventCategory.INTERNATIONAL_DAY,
            "UN global day of action for children, by children, championing children's rights."
        )
        registerFixedEvent(
            "fix_nov_26", "Constitution Day (Samvidhan Divas)", 11, 26,
            EventCategory.NATIONAL_DAY,
            "Commemorating the adoption of the Constitution of India on November 26, 1949."
        )

        // --- DECEMBER ---
        registerFixedEvent(
            "fix_dec_01", "World AIDS Day", 12, 1,
            EventCategory.AWARENESS_DAY,
            "Uniting in the fight against HIV, supporting people living with HIV, and remembering those who died."
        )
        registerFixedEvent(
            "fix_dec_02", "National Pollution Control Day", 12, 2,
            EventCategory.AWARENESS_DAY,
            "Honoring victims of the Bhopal gas tragedy and spreading awareness on environmental pollution control."
        )
        registerFixedEvent(
            "fix_dec_03", "International Day of Persons with Disabilities", 12, 3,
            EventCategory.INTERNATIONAL_DAY,
            "Promoting rights, inclusion, and well-being of persons with disabilities in all spheres of society."
        )
        registerFixedEvent(
            "fix_dec_04", "Indian Navy Day", 12, 4,
            EventCategory.NATIONAL_DAY,
            "Commemorating the courageous attack on Karachi harbor during the 1971 war and honoring naval heroes."
        )
        registerFixedEvent(
            "fix_dec_05", "World Soil Day", 12, 5,
            EventCategory.AWARENESS_DAY,
            "Focusing attention on the importance of healthy soil and sustainable management of soil resources."
        )
        registerFixedEvent(
            "fix_dec_07", "Armed Forces Flag Day", 12, 7,
            EventCategory.NATIONAL_DAY,
            "Honoring soldiers, sailors, and airmen of India and collecting funds for personnel welfare."
        )
        registerFixedEvent(
            "fix_dec_10", "Human Rights Day", 12, 10,
            EventCategory.INTERNATIONAL_DAY,
            "Commemorating the adoption of the Universal Declaration of Human Rights in 1948."
        )
        registerFixedEvent(
            "fix_dec_11", "UNICEF Day", 12, 11,
            EventCategory.INTERNATIONAL_DAY,
            "Marking the founding of UNICEF to save children's lives, defend their rights, and help fulfill their potential."
        )
        registerFixedEvent(
            "fix_dec_14", "National Energy Conservation Day", 12, 14,
            EventCategory.AWARENESS_DAY,
            "Highlighting the importance of energy efficiency, conservation, and sustainable power."
        )
        registerFixedEvent(
            "fix_dec_16_vijay_diwas", "Vijay Diwas", 12, 16,
            EventCategory.NATIONAL_DAY,
            "Commemorating India's historic military victory in the 1971 Indo-Pak war and paying homage to martyrs.",
            traditionOrRegion = "Pan-India"
        )
        registerFixedEvent(
            "fix_dec_22", "National Mathematics Day", 12, 22,
            EventCategory.NATIONAL_DAY,
            "Honoring the extraordinary mathematical genius of Srinivasa Ramanujan on his birth anniversary."
        )
        registerFixedEvent(
            "fix_dec_23", "National Farmers' Day (Kisan Diwas)", 12, 23,
            EventCategory.NATIONAL_DAY,
            "Celebrating the invaluable contributions of farmers on the birth anniversary of Chaudhary Charan Singh."
        )
        registerFixedEvent(
            "fix_dec_24", "National Consumer Day", 12, 24,
            EventCategory.NATIONAL_DAY,
            "Commemorating the enactment of the Consumer Protection Act, 1986 in India."
        )
        registerFixedEvent(
            "fix_dec_25", "Christmas", 12, 25,
            EventCategory.RELIGIOUS_FESTIVAL,
            "Joyous worldwide Christian festival celebrating the birth of Jesus Christ with love, peace, and goodwill."
        )
        registerFixedEvent(
            "fix_dec_25_gov", "Good Governance Day", 12, 25,
            EventCategory.NATIONAL_DAY,
            "Commemorating the birth anniversary of former Prime Minister Atal Bihari Vajpayee."
        )
        registerFixedEvent(
            "fix_dec_31", "New Year's Eve", 12, 31,
            EventCategory.PUBLIC_HOLIDAY,
            "Reflecting on the passing year and celebrating the eve of the new year with loved ones."
        )


        // =========================================================================
        // 2. YEAR 2025: LUNISOLAR & MULTI-RELIGIOUS FESTIVALS
        // =========================================================================
        registerYearEvent("2025_vasant", "Vasant Panchami (Saraswati Puja)", 2025, 2, 2, EventCategory.HINDU_FESTIVAL, "Auspicious festival heralding the arrival of spring and worshiping Maa Saraswati, goddess of wisdom and arts.")
        registerYearEvent("2025_shivaratri", "Maha Shivaratri", 2025, 2, 26, EventCategory.HINDU_FESTIVAL, "The Great Night of Shiva, observed with meditation, fasting, chanting, and night-long vigil.")
        registerYearEvent("2025_ramadan", "Ramadan Begins", 2025, 3, 1, EventCategory.RELIGIOUS_FESTIVAL, "Holy month of fasting, deep prayer, spiritual reflection, and community charity in Islam.")
        registerYearEvent("2025_holika", "Holika Dahan (Chhoti Holi)", 2025, 3, 13, EventCategory.HINDU_FESTIVAL, "Sacred bonfire celebrating the triumph of devotion and righteousness over evil.")
        registerYearEvent("2025_holi", "Holi (Festival of Colors)", 2025, 3, 14, EventCategory.HINDU_FESTIVAL, "Vibrant festival celebrating love, forgiveness, new spring life, and colorful joy.")
        registerYearEvent("2025_ugadi", "Ugadi / Gudi Padwa", 2025, 3, 30, EventCategory.HINDU_FESTIVAL, "Lunisolar New Year celebrated across Maharashtra, Andhra Pradesh, Telangana, and Karnataka.")
        registerYearEvent("2025_eid_fitr", "Eid al-Fitr", 2025, 3, 31, EventCategory.RELIGIOUS_FESTIVAL, "Joyous Islamic festival of breaking the fast, marking the conclusion of the holy month of Ramadan.")
        registerYearEvent("2025_ram_navami", "Rama Navami", 2025, 4, 6, EventCategory.HINDU_FESTIVAL, "Celebration of the birth of Lord Rama, the embodiment of truth and righteousness (Dharma).")
        registerYearEvent("2025_mahavir", "Mahavir Jayanti", 2025, 4, 10, EventCategory.RELIGIOUS_FESTIVAL, "Most important religious festival in Jainism celebrating the birth of Bhagwan Mahavira, pioneer of non-violence (Ahimsa).")
        registerYearEvent("2025_hanuman", "Hanuman Jayanti", 2025, 4, 12, EventCategory.HINDU_FESTIVAL, "Celebrating the birth of Lord Hanuman, the supreme symbol of devotion, courage, and selfless service.")
        registerYearEvent("2025_vaisakhi", "Vaisakhi / Baisakhi", 2025, 4, 13, EventCategory.CULTURAL_EVENT, "Harvest festival of Punjab and celebration of the creation of the Khalsa Panth in Sikh tradition.")
        registerYearEvent("2025_good_friday", "Good Friday", 2025, 4, 18, EventCategory.RELIGIOUS_FESTIVAL, "Christian day commemorating the crucifixion of Jesus Christ at Calvary.")
        registerYearEvent("2025_easter", "Easter Sunday", 2025, 4, 20, EventCategory.RELIGIOUS_FESTIVAL, "Christian festival celebrating the glorious resurrection of Jesus Christ from the dead.")
        registerYearEvent("2025_akshaya", "Akshaya Tritiya", 2025, 4, 30, EventCategory.HINDU_FESTIVAL, "Highly auspicious day symbolizing unending prosperity, good fortune, and new endeavors.")
        registerYearEvent("2025_buddha", "Buddha Purnima (Vesak)", 2025, 5, 12, EventCategory.RELIGIOUS_FESTIVAL, "Commemorating the birth, enlightenment (Bodhi), and Parinirvana of Gautama Buddha.")
        registerYearEvent("2025_eid_adha", "Eid al-Adha (Bakrid)", 2025, 6, 6, EventCategory.RELIGIOUS_FESTIVAL, "Feast of Sacrifice honoring the willingness of Prophet Ibrahim to obey God's command.")
        registerYearEvent("2025_muharram", "Muharram (Ashura)", 2025, 7, 6, EventCategory.RELIGIOUS_FESTIVAL, "Solemn observance marking the start of the Islamic New Year and the martyrdom of Imam Hussain.")
        registerYearEvent("2025_guru_purnima", "Guru Purnima", 2025, 7, 10, EventCategory.HINDU_FESTIVAL, "Expressing heartfelt gratitude and reverence to spiritual teachers, masters, and mentors.")
        registerYearEvent("2025_rakhi", "Raksha Bandhan", 2025, 8, 9, EventCategory.HINDU_FESTIVAL, "Celebrating the sacred bond of love, care, and protection between brothers and sisters.")
        registerYearEvent("2025_janmashtami", "Krishna Janmashtami", 2025, 8, 16, EventCategory.HINDU_FESTIVAL, "Joyful celebration of the birth of Lord Krishna, observed with devotional singing, fasting, and Dahi Handi.")
        registerYearEvent("2025_ganesh", "Ganesh Chaturthi", 2025, 8, 27, EventCategory.HINDU_FESTIVAL, "Grand festival welcoming Lord Ganesha, the remover of obstacles and god of wisdom.")
        registerYearEvent("2025_onam", "Onam (Thiruvonam)", 2025, 9, 5, EventCategory.CULTURAL_EVENT, "Spectacular harvest festival of Kerala celebrating the annual return of the legendary King Mahabali.")
        registerYearEvent("2025_navratri", "Shardiya Navratri Begins", 2025, 9, 22, EventCategory.HINDU_FESTIVAL, "Nine sacred nights dedicated to the nine forms of the divine Mother Durga (Shakti).")
        registerYearEvent("2025_durga_puja", "Durga Puja (Maha Saptami)", 2025, 9, 30, EventCategory.HINDU_FESTIVAL, "Majestic festival celebrating Maa Durga's victory over Mahishasura.")
        registerYearEvent("2025_dussehra", "Dussehra (Vijayadashami)", 2025, 10, 2, EventCategory.HINDU_FESTIVAL, "Celebration of the victory of good over evil, Lord Rama's victory over Ravana, and Maa Durga's triumph.")
        registerYearEvent("2025_karwa_chauth", "Karwa Chauth", 2025, 10, 10, EventCategory.HINDU_FESTIVAL, "Fasting from sunrise to moonrise observed for love, safety, and longevity of spouses.")
        registerYearEvent("2025_dhanteras", "Dhanteras", 2025, 10, 18, EventCategory.HINDU_FESTIVAL, "Festival of wealth and health, celebrating Lord Dhanvantari, the physician of the gods.")
        registerYearEvent("2025_diwali", "Diwali (Deepavali - Lakshmi Puja)", 2025, 10, 20, EventCategory.HINDU_FESTIVAL, "The Festival of Lights celebrating the victory of light over darkness and knowledge over ignorance.")
        registerYearEvent("2025_govardhan", "Govardhan Puja / Annakut", 2025, 10, 21, EventCategory.HINDU_FESTIVAL, "Celebrating Lord Krishna lifting the Govardhan Hill to protect the people of Braj.")
        registerYearEvent("2025_bhai_dooj", "Bhai Dooj", 2025, 10, 22, EventCategory.HINDU_FESTIVAL, "Cherished festival celebrating the affection and bond between brothers and sisters.")
        registerYearEvent("2025_chhath", "Chhath Puja", 2025, 10, 26, EventCategory.HINDU_FESTIVAL, "Ancient Vedic festival expressing deep gratitude to the Sun God (Surya) and Chhathi Maiya.")
        registerYearEvent("2025_guru_nanak", "Guru Nanak Gurpurab", 2025, 11, 5, EventCategory.RELIGIOUS_FESTIVAL, "Prakash Utsav celebrating the birth of Guru Nanak Dev Ji, founder of Sikhism.")
        registerYearEvent("2025_hanukkah", "Hanukkah Begins", 2025, 12, 14, EventCategory.RELIGIOUS_FESTIVAL, "Jewish eight-day Festival of Lights commemorating the rededication of the Second Temple.")


        // =========================================================================
        // 3. YEAR 2026: LUNISOLAR, MOVABLE & INDIAN RELIGIOUS / CULTURAL FESTIVALS
        // =========================================================================
        registerYearEvent("2026_pausha_purnima", "Pausha Purnima", 2026, 1, 3, EventCategory.HINDU_FESTIVAL, "Sacred Pausha Purnima observance with holy dip, charity, and Satyanarayan fasting.", traditionOrRegion = "Pan-India")
        registerYearEvent("2026_guru_gobind", "Guru Gobind Singh Jayanti", 2026, 1, 5, EventCategory.RELIGIOUS_FESTIVAL, "Prakash Utsav of the tenth Sikh Guru, warrior-poet, and founder of the Khalsa.", traditionOrRegion = "Sikh")
        registerYearEvent("2026_sakat_chauth", "Sakat Chauth (Tilkut Chauth)", 2026, 1, 6, EventCategory.HINDU_FESTIVAL, "Sankashti Chaturthi fast observed by mothers for the health and prosperity of their children.", traditionOrRegion = "North India")
        registerYearEvent("2026_mauni_amavasya", "Mauni Amavasya", 2026, 1, 18, EventCategory.HINDU_FESTIVAL, "Auspicious Magha Amavasya observed with silence, meditation, and holy dips at Triveni Sangam.", traditionOrRegion = "Pan-India")
        registerYearEvent("2026_vasant", "Vasant Panchami (Saraswati Puja)", 2026, 1, 23, EventCategory.HINDU_FESTIVAL, "Spring festival dedicated to Maa Saraswati, goddess of learning, music, and creative arts.", traditionOrRegion = "Pan-India")
        registerYearEvent("2026_ratha_saptami", "Ratha Saptami (Surya Jayanti)", 2026, 1, 25, EventCategory.HINDU_FESTIVAL, "Auspicious Magha Shukla Saptami invoking Lord Surya turning his celestial chariot toward the north.", traditionOrRegion = "Pan-India")
        registerYearEvent("2026_magha_purnima", "Magha Purnima", 2026, 2, 1, EventCategory.HINDU_FESTIVAL, "Sacred Maghi Purnima bath concluding the month of Magha at Triveni Sangam Prayagraj.", traditionOrRegion = "Pan-India")
        registerYearEvent("2026_thaipusam", "Thai Poosam (Thaipusam)", 2026, 2, 1, EventCategory.HINDU_FESTIVAL, "Tamil festival honoring Lord Murugan with kavadi offerings and devotional penance on Thai Pusam.", traditionOrRegion = "Tamil Nadu / South India")
        registerYearEvent("2026_shab_barat", "Shab-e-Barat", 2026, 2, 3, EventCategory.RELIGIOUS_FESTIVAL, "Night of records, forgiveness, and prayers in the Islamic calendar.", traditionOrRegion = "Islamic")
        registerYearEvent("2026_shivaratri", "Maha Shivaratri", 2026, 2, 15, EventCategory.HINDU_FESTIVAL, "The auspicious Great Night of Lord Shiva, celebrated with meditation, fasting, and devotional prayers.", traditionOrRegion = "Pan-India")
        registerYearEvent("2026_ramadan", "Ramadan Begins", 2026, 2, 18, EventCategory.RELIGIOUS_FESTIVAL, "Start of the Islamic holy month of fasting, reflection, self-discipline, and charity.", traditionOrRegion = "Islamic")
        registerYearEvent("2026_ash_wed", "Ash Wednesday", 2026, 2, 18, EventCategory.RELIGIOUS_FESTIVAL, "First day of Lent in the Christian calendar, observed with fasting, prayer, and repentance.", traditionOrRegion = "Christian")
        registerYearEvent("2026_holika", "Holika Dahan", 2026, 3, 3, EventCategory.HINDU_FESTIVAL, "Traditional evening bonfire celebrating the victory of Bhakta Prahlada's devotion over demonic forces.", traditionOrRegion = "Pan-India")
        registerYearEvent("2026_holi", "Holi (Festival of Colors)", 2026, 3, 4, EventCategory.HINDU_FESTIVAL, "The joyous festival of colors, welcoming spring, sharing sweets, and strengthening friendships.", traditionOrRegion = "Pan-India")
        registerYearEvent("2026_hola_mohalla", "Hola Mohalla", 2026, 3, 5, EventCategory.CULTURAL_EVENT, "Sikh martial festival founded by Guru Gobind Singh Ji featuring Gatka, horse riding, and poetry at Anandpur Sahib.", traditionOrRegion = "Punjab / Sikh")
        registerYearEvent("2026_sheetala_ashtami", "Sheetala Ashtami (Basoda)", 2026, 3, 11, EventCategory.HINDU_FESTIVAL, "Prayers to Goddess Sheetala for health and protection against seasonal ailments, offering Basoda (cold food).", traditionOrRegion = "North / West India")
        registerYearEvent("2026_shab_e_qadr", "Laylat al-Qadr (Shab-e-Qadr)", 2026, 3, 16, EventCategory.RELIGIOUS_FESTIVAL, "The Night of Decree and Power in the Islamic calendar, commemorating when the Holy Quran was revealed.", traditionOrRegion = "Islamic")
        registerYearEvent("2026_ugadi", "Ugadi / Gudi Padwa / Cheti Chand", 2026, 3, 19, EventCategory.HINDU_FESTIVAL, "Lunisolar New Year marked by raising the Gudi in Maharashtra and tasting Ugadi Pachadi in the Deccan (also observed as Navreh / Sajibu Cheiraoba).", traditionOrRegion = "South / West India")
        registerYearEvent("2026_eid_fitr", "Eid al-Fitr", 2026, 3, 20, EventCategory.RELIGIOUS_FESTIVAL, "Celebration concluding Ramadan with community prayers, feasts, family visits, and charity.", traditionOrRegion = "Islamic")
        registerYearEvent("2026_gangaur", "Gangaur", 2026, 3, 21, EventCategory.HINDU_FESTIVAL, "Spring festival of Shiva-Gauri worship celebrated with traditional devotion, songs, and ghewar sweets.", traditionOrRegion = "North / West India")
        registerYearEvent("2026_ram_navami", "Rama Navami", 2026, 3, 27, EventCategory.HINDU_FESTIVAL, "Sacred day commemorating the appearance of Lord Rama, the ideal king and upholder of truth.", traditionOrRegion = "Pan-India")
        registerYearEvent("2026_mahavir", "Mahavir Jayanti", 2026, 3, 31, EventCategory.RELIGIOUS_FESTIVAL, "Celebrating the auspicious birth anniversary of Lord Mahavira, the 24th Tirthankara of Jainism.", traditionOrRegion = "Jain")
        registerYearEvent("2026_maundy_thursday", "Maundy Thursday", 2026, 4, 2, EventCategory.RELIGIOUS_FESTIVAL, "Christian Holy Week commemoration of the Last Supper of Jesus Christ with his disciples.", traditionOrRegion = "Christian")
        registerYearEvent("2026_hanuman", "Hanuman Jayanti", 2026, 4, 2, EventCategory.HINDU_FESTIVAL, "Birth celebration of Lord Hanuman, embodiment of courage, humility, and unwavering loyalty.", traditionOrRegion = "Pan-India")
        registerYearEvent("2026_good_friday", "Good Friday", 2026, 4, 3, EventCategory.RELIGIOUS_FESTIVAL, "Christian holy day commemorating the passion and crucifixion of Jesus Christ.", traditionOrRegion = "Christian")
        registerYearEvent("2026_easter", "Easter Sunday", 2026, 4, 5, EventCategory.RELIGIOUS_FESTIVAL, "The pinnacle of the Christian liturgical year celebrating the resurrection of Jesus Christ.", traditionOrRegion = "Christian")
        registerYearEvent("2026_vaisakhi", "Vaisakhi / Baisakhi", 2026, 4, 13, EventCategory.CULTURAL_EVENT, "Celebrating the spring harvest and the historic creation of the Khalsa by Guru Gobind Singh Ji in 1699.", traditionOrRegion = "North India")
        registerYearEvent("2026_akshaya", "Akshaya Tritiya", 2026, 4, 20, EventCategory.HINDU_FESTIVAL, "Auspicious day of eternal wealth, new beginnings, gold purchases, and charity.", traditionOrRegion = "Pan-India")
        registerYearEvent("2026_buddha", "Buddha Purnima (Vesak)", 2026, 5, 2, EventCategory.RELIGIOUS_FESTIVAL, "Sacred day celebrating the birth, enlightenment, and Maha Samadhi of Lord Buddha.", traditionOrRegion = "Buddhist")
        registerYearEvent("2026_eid_adha", "Eid al-Adha (Bakrid)", 2026, 5, 27, EventCategory.RELIGIOUS_FESTIVAL, "Major Islamic holiday celebrating faith, sacrifice, and sharing meals with the needy.", traditionOrRegion = "Islamic")
        registerYearEvent("2026_vat_savitri", "Vat Savitri Vrat", 2026, 6, 15, EventCategory.HINDU_FESTIVAL, "Fasting and banyan tree worship by married women praying for marital bliss and partner longevity.", traditionOrRegion = "North India")
        registerYearEvent("2026_ganga_dussehra", "Ganga Dussehra", 2026, 6, 24, EventCategory.HINDU_FESTIVAL, "Day commemorating the descent of the sacred River Ganga upon Earth.", traditionOrRegion = "North India")
        registerYearEvent("2026_muharram", "Muharram (Ashura)", 2026, 6, 25, EventCategory.RELIGIOUS_FESTIVAL, "First month of the Islamic calendar and solemn day of remembrance for Hazrat Imam Hussain.", traditionOrRegion = "Islamic")
        registerYearEvent("2026_nirjala_ekadashi", "Nirjala Ekadashi", 2026, 6, 26, EventCategory.HINDU_FESTIVAL, "The most sacred waterless fast among the 24 Ekadashis, dedicated to Lord Vishnu.", traditionOrRegion = "Pan-India")
        registerYearEvent("2026_vat_purnima", "Vat Purnima", 2026, 6, 29, EventCategory.HINDU_FESTIVAL, "Jyeshtha Purnima banyan tree worship by married women for marital harmony.", traditionOrRegion = "West / South India")
        registerYearEvent("2026_rath_yatra", "Jagannath Ratha Yatra", 2026, 7, 16, EventCategory.HINDU_FESTIVAL, "Famous grand chariot procession of Lord Jagannath, Balabhadra, and Subhadra in Puri.", traditionOrRegion = "East / Pan-India")
        registerYearEvent("2026_devshayani", "Devshayani Ekadashi", 2026, 7, 25, EventCategory.HINDU_FESTIVAL, "Marks the start of the auspicious four-month spiritual period of Chaturmas.", traditionOrRegion = "Pan-India")
        registerYearEvent("2026_guru_purnima", "Guru Purnima", 2026, 7, 29, EventCategory.HINDU_FESTIVAL, "Day of heartfelt reverence honoring all teachers, gurus, and Ved Vyasa, author of the Mahabharata.", traditionOrRegion = "Pan-India")
        registerYearEvent("2026_hariyali_amavasya", "Hariyali Amavasya", 2026, 8, 12, EventCategory.HINDU_FESTIVAL, "New moon day of Shravana celebrated with reverence to nature, tree planting, and prayers to Lord Shiva.", traditionOrRegion = "North / Central India")
        registerYearEvent("2026_hariyali_teej", "Hariyali Teej", 2026, 8, 15, EventCategory.HINDU_FESTIVAL, "Monsoon festival celebrated with swings, songs, and prayers to Shiva and Parvati.", traditionOrRegion = "North India")
        registerYearEvent("2026_parsi_new_year", "Parsi New Year (Shahenshahi)", 2026, 8, 15, EventCategory.CULTURAL_EVENT, "Navroz celebration observed according to the Shahenshahi calendar.", traditionOrRegion = "Parsi")
        registerYearEvent("2026_nag_panchami", "Nag Panchami", 2026, 8, 17, EventCategory.HINDU_FESTIVAL, "Traditional day of serpent deity reverence, protection, and gratitude to nature.", traditionOrRegion = "Pan-India")
        registerYearEvent("2026_varalakshmi", "Varalakshmi Vratam", 2026, 8, 21, EventCategory.HINDU_FESTIVAL, "Sacred fast and prayers observed by women invoking the blessings of Goddess Lakshmi.", traditionOrRegion = "South India")
        registerYearEvent("2026_milad_nabi", "Milad-un-Nabi (Eid-e-Milad)", 2026, 8, 25, EventCategory.RELIGIOUS_FESTIVAL, "Birth anniversary of Prophet Muhammad observed with prayers, sermons, and feasts.", traditionOrRegion = "Islamic")
        registerYearEvent("2026_rakhi", "Raksha Bandhan", 2026, 8, 28, EventCategory.HINDU_FESTIVAL, "Sisters tie sacred threads (Rakhi) on their brothers' wrists symbolizing mutual love, blessing, and care.", traditionOrRegion = "Pan-India")
        registerYearEvent("2026_onam", "Onam (Thiruvonam)", 2026, 8, 29, EventCategory.CULTURAL_EVENT, "The grand finale of Kerala's 10-day harvest festival with floral carpets (Pookalam) and Onasadya feast.", traditionOrRegion = "Kerala")
        registerYearEvent("2026_kajari_teej", "Kajari Teej (Badi Teej)", 2026, 8, 30, EventCategory.HINDU_FESTIVAL, "Celebrated with prayers to Goddess Parvati, swings, and folk processions.", traditionOrRegion = "North / Central India")
        registerYearEvent("2026_janmashtami", "Krishna Janmashtami", 2026, 9, 4, EventCategory.HINDU_FESTIVAL, "Midnight celebration of the birth of Lord Krishna with devotional chants, fasting, and joy.", traditionOrRegion = "Pan-India")
        registerYearEvent("2026_paryushan_start", "Paryushan Parva Begins", 2026, 9, 7, EventCategory.RELIGIOUS_FESTIVAL, "Start of the 8-to-10-day Jain festival of introspection, self-discipline, and fasting.", traditionOrRegion = "Jain")
        registerYearEvent("2026_hartalika_teej", "Hartalika Teej", 2026, 9, 14, EventCategory.HINDU_FESTIVAL, "Rigorous day-long fast observed by women honoring Goddess Parvati and Lord Shiva.", traditionOrRegion = "North / Central India")
        registerYearEvent("2026_ganesh", "Ganesh Chaturthi (Vinayaka Chavithi)", 2026, 9, 14, EventCategory.HINDU_FESTIVAL, "Joyous festival welcoming Lord Ganesha into homes with modaks, prayers, and cultural celebrations.", traditionOrRegion = "Pan-India")
        registerYearEvent("2026_samvatsari", "Samvatsari (Michhami Dukkadam)", 2026, 9, 15, EventCategory.RELIGIOUS_FESTIVAL, "Grand culmination of Paryushan, asking forgiveness from all living beings.", traditionOrRegion = "Jain")
        registerYearEvent("2026_radha_ashtami", "Radha Ashtami", 2026, 9, 19, EventCategory.HINDU_FESTIVAL, "Auspicious appearance day of Shri Radha Rani, celebrated with devotional bhajans and joy.", traditionOrRegion = "Pan-India")
        registerYearEvent("2026_anant_chaturdashi", "Anant Chaturdashi (Ganesh Visarjan)", 2026, 9, 24, EventCategory.HINDU_FESTIVAL, "Final day of Ganeshotsav with grand immersion processions and Anant Vrata.", traditionOrRegion = "Pan-India")
        registerYearEvent("2026_sarva_pitru", "Sarva Pitru Amavasya (Mahalaya)", 2026, 10, 10, EventCategory.HINDU_FESTIVAL, "Culmination of Pitru Paksha honoring ancestors, and invocation of Maa Durga.", traditionOrRegion = "Pan-India")
        registerYearEvent("2026_navratri", "Shardiya Navratri Begins", 2026, 10, 11, EventCategory.HINDU_FESTIVAL, "Ghatasthapana marking nine auspicious days of music, dance (Garba/Dandiya), and Durga worship.", traditionOrRegion = "Pan-India")
        registerYearEvent("2026_durga_ashtami", "Maha Ashtami / Durga Puja", 2026, 10, 19, EventCategory.HINDU_FESTIVAL, "Auspicious day of Durga Puja celebrating Maa Durga's supreme spiritual power.", traditionOrRegion = "East / Pan-India")
        registerYearEvent("2026_bathukamma", "Saddula Bathukamma", 2026, 10, 19, EventCategory.CULTURAL_EVENT, "Culmination of the 9-day Bathukamma festival widely observed across Telangana, honoring Mother Gauri with vibrant floral arrangements.", traditionOrRegion = "Telangana, India")
        registerYearEvent("2026_dussehra", "Dussehra (Vijayadashami)", 2026, 10, 20, EventCategory.HINDU_FESTIVAL, "Grand celebration of good conquering evil, burning effigies of Ravana and blessing new ventures.", traditionOrRegion = "Pan-India")
        registerYearEvent("2026_sharad_purnima", "Sharad Purnima (Kojagari Puja)", 2026, 10, 25, EventCategory.HINDU_FESTIVAL, "Full moon night dedicated to Goddess Lakshmi and preparing traditional kheer.", traditionOrRegion = "Pan-India")
        registerYearEvent("2026_karwa_chauth", "Karwa Chauth", 2026, 10, 29, EventCategory.HINDU_FESTIVAL, "Traditional fast observed with devotion for the longevity and health of life partners.", traditionOrRegion = "North / West India")
        registerYearEvent("2026_ahoi_ashtami", "Ahoi Ashtami", 2026, 11, 2, EventCategory.HINDU_FESTIVAL, "Fast observed by mothers for the protection, health, and well-being of their children.", traditionOrRegion = "North India")
        registerYearEvent("2026_govatsa", "Govatsa Dwadashi (Vagh Baras)", 2026, 11, 5, EventCategory.HINDU_FESTIVAL, "Day honoring cows and maternal love, marking the gentle start of Diwali festivities.", traditionOrRegion = "West / Pan-India")
        registerYearEvent("2026_dhanteras", "Dhanteras", 2026, 11, 6, EventCategory.HINDU_FESTIVAL, "First day of Diwali festivities honoring wealth, auspicious metal purchases, and Lord Dhanvantari.", traditionOrRegion = "Pan-India")
        registerYearEvent("2026_naraka_chaturdashi", "Naraka Chaturdashi (Chhoti Diwali)", 2026, 11, 7, EventCategory.HINDU_FESTIVAL, "Early morning oil bath, lighting of lamps, and celebration of the victory over Narakasura.", traditionOrRegion = "Pan-India")
        registerYearEvent("2026_diwali", "Diwali (Deepavali - Lakshmi Puja)", 2026, 11, 8, EventCategory.HINDU_FESTIVAL, "The grand Festival of Lights celebrating joy, home illuminations, Lakshmi Puja, and new beginnings.", traditionOrRegion = "Pan-India")
        registerYearEvent("2026_bandi_chhor", "Bandi Chhor Divas", 2026, 11, 8, EventCategory.RELIGIOUS_FESTIVAL, "Sikh celebration commemorating the historic release of Guru Hargobind Ji and 52 kings from Gwalior Fort.", traditionOrRegion = "Sikh")
        registerYearEvent("2026_mahavir_nirvana", "Mahavira Nirvana (Jain Diwali)", 2026, 11, 8, EventCategory.RELIGIOUS_FESTIVAL, "Jain festival celebrating the attainment of Moksha by Bhagwan Mahavira with sacred lamps (Divo).", traditionOrRegion = "Jain")
        registerYearEvent("2026_govardhan", "Govardhan Puja / Annakut", 2026, 11, 9, EventCategory.HINDU_FESTIVAL, "Expressing gratitude to Mother Nature and honoring Lord Krishna's protection of living beings.", traditionOrRegion = "Pan-India")
        registerYearEvent("2026_bhai_dooj", "Bhai Dooj", 2026, 11, 10, EventCategory.HINDU_FESTIVAL, "Celebration of sibling affection, gift sharing, and blessings for happiness and long life.", traditionOrRegion = "Pan-India")
        registerYearEvent("2026_chhath_nahay_khay", "Chhath Puja — Nahay Khay", 2026, 11, 13, EventCategory.HINDU_FESTIVAL, "First day of Chhath Puja: ritual purification bath and sanctified satvik meal.", traditionOrRegion = "North / East India")
        registerYearEvent("2026_chhath_kharna", "Chhath Puja — Kharna / Lohanda", 2026, 11, 14, EventCategory.HINDU_FESTIVAL, "Second day of Chhath Puja: day-long nirjala fast followed by evening prasad of gur kheer and roti.", traditionOrRegion = "North / East India")
        registerYearEvent("2026_chhath", "Chhath Puja — Sandhya Arghya", 2026, 11, 15, EventCategory.HINDU_FESTIVAL, "Third day of Chhath: sacred evening offerings (Sanjhiya Arghya) to the setting Sun at river ghats.", traditionOrRegion = "North / East India")
        registerYearEvent("2026_skanda_sashti", "Skanda Sashti (Soorasamharam)", 2026, 11, 15, EventCategory.HINDU_FESTIVAL, "Six-day festival celebrating Lord Murugan's victory over the demon Soorapadman in Tiruchendur and temples worldwide.", traditionOrRegion = "Tamil Nadu / South India")
        registerYearEvent("2026_chhath_usha_arghya", "Chhath Puja — Usha Arghya / Parana", 2026, 11, 16, EventCategory.HINDU_FESTIVAL, "Fourth day of Chhath: sunrise offerings (Bhorwa Arghya) to Lord Surya concluding the 36-hour fast.", traditionOrRegion = "North / East India")
        registerYearEvent("2026_akshaya_navami", "Akshaya Navami (Amla Navami)", 2026, 11, 18, EventCategory.HINDU_FESTIVAL, "Auspicious day dedicated to worshipping the sacred Amla tree, nature reverence, and charity.", traditionOrRegion = "Pan-India")
        registerYearEvent("2026_tulsi_vivah", "Devutthana Ekadashi / Tulsi Vivah", 2026, 11, 20, EventCategory.HINDU_FESTIVAL, "Awakening of Lord Vishnu concluding Chaturmas; ceremonial wedding of Tulsi.", traditionOrRegion = "Pan-India")
        registerYearEvent("2026_karthigai_deepam", "Karthigai Deepam", 2026, 11, 23, EventCategory.HINDU_FESTIVAL, "Ancient Tamil festival of lights celebrating the cosmic pillar of fire of Lord Shiva at Tiruvannamalai.", traditionOrRegion = "Tamil Nadu / South India")
        registerYearEvent("2026_dev_deepawali", "Dev Deepawali (Kartik Purnima)", 2026, 11, 24, EventCategory.HINDU_FESTIVAL, "Festival of lights of the gods in Varanasi; sacred bathing on Kartik Purnima.", traditionOrRegion = "Pan-India")
        registerYearEvent("2026_guru_nanak", "Guru Nanak Gurpurab", 2026, 11, 24, EventCategory.RELIGIOUS_FESTIVAL, "Prakash Utsav honoring the teachings of equality, selfless service, and oneness of humanity.", traditionOrRegion = "Sikh")
        registerYearEvent("2026_tegh_bahadur", "Martyrdom Day of Guru Tegh Bahadur Ji", 2026, 11, 24, EventCategory.RELIGIOUS_FESTIVAL, "Solemn historical remembrance honoring the 9th Sikh Guru's supreme sacrifice.", traditionOrRegion = "Sikh")
        registerYearEvent("2026_kalabhairav_ashtami", "Kalabhairav Ashtami (Bhairava Jayanti)", 2026, 12, 2, EventCategory.HINDU_FESTIVAL, "Appearance day of Lord Kalabhairava, protector deity and remover of fear and obstacles.", traditionOrRegion = "Pan-India")
        registerYearEvent("2026_gita_jayanti", "Gita Jayanti / Mokshada & Vaikuntha Ekadashi", 2026, 12, 20, EventCategory.HINDU_FESTIVAL, "Commemorates the revelation of the Bhagavad Gita and Vaikuntha Dwaram opening in Tirumala and Srirangam.", traditionOrRegion = "Pan-India")
        registerYearEvent("2026_dattatreya", "Dattatreya Jayanti", 2026, 12, 23, EventCategory.HINDU_FESTIVAL, "Appearance day of Lord Dattatreya, divine incarnation of Brahma, Vishnu, and Shiva on Margashirsha Purnima.", traditionOrRegion = "Pan-India")


        // =========================================================================
        // 4. YEAR 2027: LUNISOLAR, MOVABLE & INDIAN RELIGIOUS / CULTURAL FESTIVALS
        // =========================================================================
        registerYearEvent("2027_pausha_purnima", "Pausha Purnima", 2027, 1, 22, EventCategory.HINDU_FESTIVAL, "Sacred Pausha Purnima observed with holy dips, charity, and Satyanarayan fasting.", traditionOrRegion = "Pan-India")
        registerYearEvent("2027_shab_barat", "Shab-e-Barat", 2027, 1, 23, EventCategory.RELIGIOUS_FESTIVAL, "Night of records and forgiveness in the Islamic calendar.", traditionOrRegion = "Islamic")
        registerYearEvent("2027_sakat_chauth", "Sakat Chauth (Tilkut Chauth)", 2027, 1, 26, EventCategory.HINDU_FESTIVAL, "Sankashti Chaturthi fast observed by mothers for children's well-being.", traditionOrRegion = "North India")
        registerYearEvent("2027_mauni_amavasya", "Mauni Amavasya", 2027, 2, 6, EventCategory.HINDU_FESTIVAL, "Sacred Magha Amavasya observed with silence and holy bath at Triveni Sangam.", traditionOrRegion = "Pan-India")
        registerYearEvent("2027_ramadan", "Ramadan Begins", 2027, 2, 8, EventCategory.RELIGIOUS_FESTIVAL, "Start of the Islamic holy month of fasting (Roza).", traditionOrRegion = "Islamic")
        registerYearEvent("2027_ash_wed", "Ash Wednesday", 2027, 2, 10, EventCategory.RELIGIOUS_FESTIVAL, "First day of Lent in the Christian calendar.", traditionOrRegion = "Christian")
        registerYearEvent("2027_vasant", "Vasant Panchami (Saraswati Puja)", 2027, 2, 11, EventCategory.HINDU_FESTIVAL, "Honoring Maa Saraswati, learning, wisdom, and the awakening of spring.", traditionOrRegion = "Pan-India")
        registerYearEvent("2027_ratha_saptami", "Ratha Saptami (Surya Jayanti)", 2027, 2, 14, EventCategory.HINDU_FESTIVAL, "Auspicious Magha Shukla Saptami invoking Lord Surya turning his celestial chariot toward the north.", traditionOrRegion = "Pan-India")
        registerYearEvent("2027_magha_purnima", "Magha Purnima", 2027, 2, 20, EventCategory.HINDU_FESTIVAL, "Sacred Maghi Purnima marking the culmination of the holy bathing month at Triveni Sangam Prayagraj.", traditionOrRegion = "Pan-India")
        registerYearEvent("2027_thaipusam", "Thai Poosam (Thaipusam)", 2027, 2, 21, EventCategory.HINDU_FESTIVAL, "Tamil festival honoring Lord Murugan with kavadi offerings and devotional penance on Thai Pusam.", traditionOrRegion = "Tamil Nadu / South India")
        registerYearEvent("2027_shab_e_qadr", "Laylat al-Qadr (Shab-e-Qadr)", 2027, 3, 5, EventCategory.RELIGIOUS_FESTIVAL, "The Night of Decree and Power in the Islamic calendar, commemorating when the Holy Quran was revealed.", traditionOrRegion = "Islamic")
        registerYearEvent("2027_shivaratri", "Maha Shivaratri", 2027, 3, 6, EventCategory.HINDU_FESTIVAL, "Night of divine contemplation and reverence dedicated to Lord Shiva.", traditionOrRegion = "Pan-India")
        registerYearEvent("2027_eid_fitr", "Eid al-Fitr", 2027, 3, 10, EventCategory.RELIGIOUS_FESTIVAL, "Celebration of peace, community sharing, and charity marking the end of Ramadan.", traditionOrRegion = "Islamic")
        registerYearEvent("2027_holika", "Holika Dahan", 2027, 3, 22, EventCategory.HINDU_FESTIVAL, "Bonfire ritual symbolizing purity and the defeat of evil.", traditionOrRegion = "Pan-India")
        registerYearEvent("2027_holi", "Holi (Festival of Colors)", 2027, 3, 23, EventCategory.HINDU_FESTIVAL, "Joyous festival of colors, music, love, and spring renewal.", traditionOrRegion = "Pan-India")
        registerYearEvent("2027_hola_mohalla", "Hola Mohalla", 2027, 3, 24, EventCategory.CULTURAL_EVENT, "Sikh martial festival founded by Guru Gobind Singh Ji featuring Gatka, horse riding, and poetry at Anandpur Sahib.", traditionOrRegion = "Punjab / Sikh")
        registerYearEvent("2027_maundy_thursday", "Maundy Thursday", 2027, 3, 25, EventCategory.RELIGIOUS_FESTIVAL, "Christian Holy Week commemoration of the Last Supper of Jesus Christ with his disciples.", traditionOrRegion = "Christian")
        registerYearEvent("2027_good_friday", "Good Friday", 2027, 3, 26, EventCategory.RELIGIOUS_FESTIVAL, "Christian observance commemorating the passion and sacrifice of Jesus Christ.", traditionOrRegion = "Christian")
        registerYearEvent("2027_easter", "Easter Sunday", 2027, 3, 28, EventCategory.RELIGIOUS_FESTIVAL, "Celebration of hope, renewal, and the resurrection of Jesus Christ.", traditionOrRegion = "Christian")
        registerYearEvent("2027_sheetala_ashtami", "Sheetala Ashtami (Basoda)", 2027, 3, 30, EventCategory.HINDU_FESTIVAL, "Prayers to Goddess Sheetala for health and protection against seasonal ailments, offering Basoda.", traditionOrRegion = "North / West India")
        registerYearEvent("2027_ugadi", "Ugadi / Gudi Padwa / Cheti Chand", 2027, 4, 7, EventCategory.HINDU_FESTIVAL, "Traditional lunisolar New Year in Maharashtra, Andhra Pradesh, Telangana, and Karnataka (also observed as Navreh / Sajibu Cheiraoba).", traditionOrRegion = "South / West India")
        registerYearEvent("2027_gangaur", "Gangaur", 2027, 4, 10, EventCategory.HINDU_FESTIVAL, "Spring festival of Shiva-Gauri worship celebrated with traditional devotion, songs, and ghewar sweets.", traditionOrRegion = "North / West India")
        registerYearEvent("2027_vaisakhi", "Vaisakhi / Baisakhi", 2027, 4, 14, EventCategory.CULTURAL_EVENT, "Sikh festival and harvest celebration of Punjab commemorating the creation of the Khalsa.", traditionOrRegion = "North India")
        registerYearEvent("2027_ram_navami", "Rama Navami", 2027, 4, 15, EventCategory.HINDU_FESTIVAL, "Celebration of the birth of Lord Rama, exemplar of righteous conduct.", traditionOrRegion = "Pan-India")
        registerYearEvent("2027_mahavir", "Mahavir Jayanti", 2027, 4, 19, EventCategory.RELIGIOUS_FESTIVAL, "Honoring Bhagwan Mahavira, champion of peace, non-violence, and truth.", traditionOrRegion = "Jain")
        registerYearEvent("2027_hanuman", "Hanuman Jayanti", 2027, 4, 21, EventCategory.HINDU_FESTIVAL, "Celebrating the birth of Lord Hanuman, the eternal devotee.", traditionOrRegion = "Pan-India")
        registerYearEvent("2027_akshaya", "Akshaya Tritiya", 2027, 5, 9, EventCategory.HINDU_FESTIVAL, "Auspicious day for everlasting success, charitable acts, and new investments.", traditionOrRegion = "Pan-India")
        registerYearEvent("2027_eid_adha", "Eid al-Adha (Bakrid)", 2027, 5, 17, EventCategory.RELIGIOUS_FESTIVAL, "Feast of sacrifice and charity commemorating devotion to God.", traditionOrRegion = "Islamic")
        registerYearEvent("2027_buddha", "Buddha Purnima (Vesak)", 2027, 5, 20, EventCategory.RELIGIOUS_FESTIVAL, "Commemorating the life and timeless wisdom of Gautama Buddha.", traditionOrRegion = "Buddhist")
        registerYearEvent("2027_vat_savitri", "Vat Savitri Vrat", 2027, 6, 4, EventCategory.HINDU_FESTIVAL, "Fasting and banyan tree worship by married women for marital longevity.", traditionOrRegion = "North India")
        registerYearEvent("2027_ganga_dussehra", "Ganga Dussehra", 2027, 6, 14, EventCategory.HINDU_FESTIVAL, "Descent of sacred River Ganga upon Earth.", traditionOrRegion = "North India")
        registerYearEvent("2027_muharram", "Muharram (Ashura)", 2027, 6, 15, EventCategory.RELIGIOUS_FESTIVAL, "Solemn Islamic observance of sacrifice and devotion.", traditionOrRegion = "Islamic")
        registerYearEvent("2027_nirjala_ekadashi", "Nirjala Ekadashi", 2027, 6, 15, EventCategory.HINDU_FESTIVAL, "Strict waterless fast dedicated to Lord Vishnu.", traditionOrRegion = "Pan-India")
        registerYearEvent("2027_vat_purnima", "Vat Purnima", 2027, 6, 19, EventCategory.HINDU_FESTIVAL, "Jyeshtha Purnima banyan tree worship by married women.", traditionOrRegion = "West / South India")
        registerYearEvent("2027_rath_yatra", "Jagannath Ratha Yatra", 2027, 7, 5, EventCategory.HINDU_FESTIVAL, "Grand chariot festival of Lord Jagannath in Puri.", traditionOrRegion = "East / Pan-India")
        registerYearEvent("2027_devshayani", "Devshayani Ekadashi", 2027, 7, 15, EventCategory.HINDU_FESTIVAL, "Beginning of the sacred Chaturmas period.", traditionOrRegion = "Pan-India")
        registerYearEvent("2027_guru_purnima", "Guru Purnima", 2027, 7, 18, EventCategory.HINDU_FESTIVAL, "Honoring spiritual and academic teachers and mentors.", traditionOrRegion = "Pan-India")
        registerYearEvent("2027_hariyali_amavasya", "Hariyali Amavasya", 2027, 8, 2, EventCategory.HINDU_FESTIVAL, "New moon day of Shravana celebrated with reverence to nature, tree planting, and prayers to Lord Shiva.", traditionOrRegion = "North / Central India")
        registerYearEvent("2027_hariyali_teej", "Hariyali Teej", 2027, 8, 4, EventCategory.HINDU_FESTIVAL, "Monsoon festival celebrated with swings, songs, and prayers to Shiva and Parvati for marital bliss.", traditionOrRegion = "North India")
        registerYearEvent("2027_nag_panchami", "Nag Panchami", 2027, 8, 7, EventCategory.HINDU_FESTIVAL, "Traditional reverence to serpent deities.", traditionOrRegion = "Pan-India")
        registerYearEvent("2027_varalakshmi", "Varalakshmi Vratam", 2027, 8, 13, EventCategory.HINDU_FESTIVAL, "Fast and worship of Goddess Lakshmi.", traditionOrRegion = "South India")
        registerYearEvent("2027_milad_nabi", "Milad-un-Nabi (Eid-e-Milad)", 2027, 8, 15, EventCategory.RELIGIOUS_FESTIVAL, "Birth celebration of Prophet Muhammad.", traditionOrRegion = "Islamic")
        registerYearEvent("2027_parsi_new_year", "Parsi New Year (Shahenshahi)", 2027, 8, 15, EventCategory.CULTURAL_EVENT, "Parsi Shahenshahi Navroz celebration.", traditionOrRegion = "Parsi")
        registerYearEvent("2027_rakhi", "Raksha Bandhan", 2027, 8, 17, EventCategory.HINDU_FESTIVAL, "Festival celebrating the bond of protection and love between siblings.", traditionOrRegion = "Pan-India")
        registerYearEvent("2027_onam", "Onam (Thiruvonam)", 2027, 8, 18, EventCategory.CULTURAL_EVENT, "Grand harvest festival of Kerala with traditional festivities and feasts.", traditionOrRegion = "Kerala")
        registerYearEvent("2027_kajari_teej", "Kajari Teej (Badi Teej)", 2027, 8, 20, EventCategory.HINDU_FESTIVAL, "Teej festivities with prayers to Goddess Parvati, swings, and folk music.", traditionOrRegion = "North / Central India")
        registerYearEvent("2027_janmashtami", "Krishna Janmashtami", 2027, 8, 25, EventCategory.HINDU_FESTIVAL, "Celebration of Lord Krishna's divine birth.", traditionOrRegion = "Pan-India")
        registerYearEvent("2027_paryushan_start", "Paryushan Parva Begins", 2027, 8, 27, EventCategory.RELIGIOUS_FESTIVAL, "Sacred period of fasting and self-purification in Jainism.", traditionOrRegion = "Jain")
        registerYearEvent("2027_hartalika_teej", "Hartalika Teej", 2027, 9, 3, EventCategory.HINDU_FESTIVAL, "Fast dedicated to Goddess Parvati and Lord Shiva.", traditionOrRegion = "North / Central India")
        registerYearEvent("2027_ganesh", "Ganesh Chaturthi (Vinayaka Chavithi)", 2027, 9, 4, EventCategory.HINDU_FESTIVAL, "Welcoming Lord Ganesha, embodiment of intellect and good beginnings.", traditionOrRegion = "Pan-India")
        registerYearEvent("2027_samvatsari", "Samvatsari (Michhami Dukkadam)", 2027, 9, 4, EventCategory.RELIGIOUS_FESTIVAL, "Day of universal forgiveness in Jainism.", traditionOrRegion = "Jain")
        registerYearEvent("2027_radha_ashtami", "Radha Ashtami", 2027, 9, 9, EventCategory.HINDU_FESTIVAL, "Auspicious appearance day of Shri Radha Rani, celebrated with devotional joy.", traditionOrRegion = "Pan-India")
        registerYearEvent("2027_anant_chaturdashi", "Anant Chaturdashi (Ganesh Visarjan)", 2027, 9, 14, EventCategory.HINDU_FESTIVAL, "Farewell immersion of Lord Ganesha and Anant Vrata.", traditionOrRegion = "Pan-India")
        registerYearEvent("2027_sarva_pitru", "Sarva Pitru Amavasya (Mahalaya)", 2027, 9, 29, EventCategory.HINDU_FESTIVAL, "Day honoring ancestors and invocation of Maa Durga.", traditionOrRegion = "Pan-India")
        registerYearEvent("2027_navratri", "Shardiya Navratri Begins", 2027, 9, 30, EventCategory.HINDU_FESTIVAL, "Nine holy nights honoring Goddess Durga.", traditionOrRegion = "Pan-India")
        registerYearEvent("2027_durga_ashtami", "Maha Ashtami / Durga Puja", 2027, 10, 8, EventCategory.HINDU_FESTIVAL, "Peak day of Durga Puja celebrations.", traditionOrRegion = "East / Pan-India")
        registerYearEvent("2027_bathukamma", "Saddula Bathukamma", 2027, 10, 8, EventCategory.CULTURAL_EVENT, "Culmination of the 9-day Bathukamma festival widely observed across Telangana, honoring Mother Gauri with vibrant floral arrangements.", traditionOrRegion = "Telangana, India")
        registerYearEvent("2027_dussehra", "Dussehra (Vijayadashami)", 2027, 10, 9, EventCategory.HINDU_FESTIVAL, "Celebration of righteousness and triumph over evil.", traditionOrRegion = "Pan-India")
        registerYearEvent("2027_sharad_purnima", "Sharad Purnima (Kojagari Puja)", 2027, 10, 15, EventCategory.HINDU_FESTIVAL, "Full moon night of Goddess Lakshmi.", traditionOrRegion = "Pan-India")
        registerYearEvent("2027_karwa_chauth", "Karwa Chauth", 2027, 10, 18, EventCategory.HINDU_FESTIVAL, "Fasting for marital harmony and partner longevity.", traditionOrRegion = "North / West India")
        registerYearEvent("2027_ahoi_ashtami", "Ahoi Ashtami", 2027, 10, 22, EventCategory.HINDU_FESTIVAL, "Fast observed by mothers for children's well-being.", traditionOrRegion = "North India")
        registerYearEvent("2027_govatsa", "Govatsa Dwadashi (Vagh Baras)", 2027, 10, 26, EventCategory.HINDU_FESTIVAL, "Honoring cows and mother nature before Diwali.", traditionOrRegion = "West / Pan-India")
        registerYearEvent("2027_dhanteras", "Dhanteras", 2027, 10, 27, EventCategory.HINDU_FESTIVAL, "Auspicious celebration of wealth and health.", traditionOrRegion = "Pan-India")
        registerYearEvent("2027_naraka_chaturdashi", "Naraka Chaturdashi (Chhoti Diwali)", 2027, 10, 28, EventCategory.HINDU_FESTIVAL, "Day before Diwali celebrating victory over Narakasura.", traditionOrRegion = "Pan-India")
        registerYearEvent("2027_diwali", "Diwali (Deepavali - Lakshmi Puja)", 2027, 10, 29, EventCategory.HINDU_FESTIVAL, "Grand Festival of Lights illuminating homes and hearts.", traditionOrRegion = "Pan-India")
        registerYearEvent("2027_bandi_chhor", "Bandi Chhor Divas", 2027, 10, 29, EventCategory.RELIGIOUS_FESTIVAL, "Sikh celebration commemorating the historic release of Guru Hargobind Ji and 52 kings from Gwalior Fort.", traditionOrRegion = "Sikh")
        registerYearEvent("2027_mahavir_nirvana", "Mahavira Nirvana (Jain Diwali)", 2027, 10, 29, EventCategory.RELIGIOUS_FESTIVAL, "Jain festival celebrating the attainment of Moksha by Bhagwan Mahavira with sacred lamps (Divo).", traditionOrRegion = "Jain")
        registerYearEvent("2027_govardhan", "Govardhan Puja / Annakut", 2027, 10, 30, EventCategory.HINDU_FESTIVAL, "Reverence for nature and cattle protection.", traditionOrRegion = "Pan-India")
        registerYearEvent("2027_bhai_dooj", "Bhai Dooj", 2027, 10, 31, EventCategory.HINDU_FESTIVAL, "Sibling appreciation and blessings.", traditionOrRegion = "Pan-India")
        registerYearEvent("2027_chhath_nahay_khay", "Chhath Puja — Nahay Khay", 2027, 11, 2, EventCategory.HINDU_FESTIVAL, "First day of Chhath Puja: ritual purification bath and sanctified satvik meal.", traditionOrRegion = "North / East India")
        registerYearEvent("2027_chhath_kharna", "Chhath Puja — Kharna / Lohanda", 2027, 11, 3, EventCategory.HINDU_FESTIVAL, "Second day of Chhath Puja: day-long fast concluded with evening prasad of gur kheer and roti.", traditionOrRegion = "North / East India")
        registerYearEvent("2027_chhath", "Chhath Puja — Sandhya Arghya", 2027, 11, 4, EventCategory.HINDU_FESTIVAL, "Third day of Chhath: sacred evening offerings (Sanjhiya Arghya) to the setting Sun at river ghats.", traditionOrRegion = "North / East India")
        registerYearEvent("2027_skanda_sashti", "Skanda Sashti (Soorasamharam)", 2027, 11, 4, EventCategory.HINDU_FESTIVAL, "Six-day festival celebrating Lord Murugan's victory over the demon Soorapadman in Tiruchendur and temples worldwide.", traditionOrRegion = "Tamil Nadu / South India")
        registerYearEvent("2027_chhath_usha_arghya", "Chhath Puja — Usha Arghya / Parana", 2027, 11, 5, EventCategory.HINDU_FESTIVAL, "Fourth day of Chhath: sunrise offerings (Bhorwa Arghya) to Lord Surya concluding the 36-hour fast.", traditionOrRegion = "North / East India")
        registerYearEvent("2027_akshaya_navami", "Akshaya Navami (Amla Navami)", 2027, 11, 8, EventCategory.HINDU_FESTIVAL, "Auspicious day dedicated to worshipping the sacred Amla tree and charity.", traditionOrRegion = "Pan-India")
        registerYearEvent("2027_tulsi_vivah", "Devutthana Ekadashi / Tulsi Vivah", 2027, 11, 10, EventCategory.HINDU_FESTIVAL, "Awakening of Lord Vishnu and Tulsi Vivah.", traditionOrRegion = "Pan-India")
        registerYearEvent("2027_karthigai_deepam", "Karthigai Deepam", 2027, 11, 12, EventCategory.HINDU_FESTIVAL, "Ancient Tamil festival of lights celebrating the cosmic pillar of fire of Lord Shiva at Tiruvannamalai.", traditionOrRegion = "Tamil Nadu / South India")
        registerYearEvent("2027_dev_deepawali", "Dev Deepawali", 2027, 11, 13, EventCategory.HINDU_FESTIVAL, "Festival of lights of the gods celebrated at the ghats of Varanasi on Kartik Purnima eve.", traditionOrRegion = "North India")
        registerYearEvent("2027_kartik_purnima", "Kartik Purnima", 2027, 11, 14, EventCategory.HINDU_FESTIVAL, "Sacred full moon of Kartik month observed with holy river baths, lamps, and charity.", traditionOrRegion = "Pan-India")
        registerYearEvent("2027_guru_nanak", "Guru Nanak Gurpurab", 2027, 11, 14, EventCategory.RELIGIOUS_FESTIVAL, "Prakash Utsav of Guru Nanak Dev Ji.", traditionOrRegion = "Sikh")
        registerYearEvent("2027_kalabhairav_ashtami", "Kalabhairav Ashtami (Bhairava Jayanti)", 2027, 11, 21, EventCategory.HINDU_FESTIVAL, "Appearance day of Lord Kalabhairava, protector deity.", traditionOrRegion = "Pan-India")
        registerYearEvent("2027_gita_jayanti", "Gita Jayanti / Mokshada & Vaikuntha Ekadashi", 2027, 12, 9, EventCategory.HINDU_FESTIVAL, "Commemoration of the Bhagavad Gita revelation and Vaikuntha Dwaram opening.", traditionOrRegion = "Pan-India")
        registerYearEvent("2027_dattatreya", "Dattatreya Jayanti", 2027, 12, 13, EventCategory.HINDU_FESTIVAL, "Appearance day of Lord Dattatreya, divine incarnation of Brahma, Vishnu, and Shiva on Margashirsha Purnima.", traditionOrRegion = "Pan-India")
        registerYearEvent("2027_tegh_bahadur", "Martyrdom Day of Guru Tegh Bahadur Ji", 2027, 12, 13, EventCategory.RELIGIOUS_FESTIVAL, "Remembrance of the 9th Sikh Guru's supreme sacrifice.", traditionOrRegion = "Sikh")
        registerYearEvent("2027_guru_gobind", "Guru Gobind Singh Jayanti", 2027, 12, 25, EventCategory.RELIGIOUS_FESTIVAL, "Prakash Utsav honoring the tenth Sikh Guru.", traditionOrRegion = "Sikh")


        // =========================================================================
        // 5. YEAR 2028: LUNISOLAR, MOVABLE & INDIAN RELIGIOUS / CULTURAL FESTIVALS
        // =========================================================================
        registerYearEvent("2028_pausha_purnima", "Pausha Purnima", 2028, 1, 11, EventCategory.HINDU_FESTIVAL, "Sacred Pausha Purnima observed with holy bath, charity, and Satyanarayan fasting.", traditionOrRegion = "Pan-India")
        registerYearEvent("2028_shab_barat", "Shab-e-Barat", 2028, 1, 12, EventCategory.RELIGIOUS_FESTIVAL, "Night of records and forgiveness in the Islamic calendar.", traditionOrRegion = "Islamic")
        registerYearEvent("2028_guru_gobind", "Guru Gobind Singh Jayanti", 2028, 1, 13, EventCategory.RELIGIOUS_FESTIVAL, "Prakash Utsav honoring the 10th Sikh Guru.", traditionOrRegion = "Sikh")
        registerYearEvent("2028_sakat_chauth", "Sakat Chauth (Tilkut Chauth)", 2028, 1, 15, EventCategory.HINDU_FESTIVAL, "Sankashti Chaturthi fast observed by mothers for children's well-being.", traditionOrRegion = "North India")
        registerYearEvent("2028_mauni_amavasya", "Mauni Amavasya", 2028, 1, 26, EventCategory.HINDU_FESTIVAL, "Sacred Magha Amavasya observed with silence and holy bath at Triveni Sangam.", traditionOrRegion = "Pan-India")
        registerYearEvent("2028_ramadan", "Ramadan Begins", 2028, 1, 28, EventCategory.RELIGIOUS_FESTIVAL, "Start of the Islamic holy month of fasting (Roza).", traditionOrRegion = "Islamic")
        registerYearEvent("2028_vasant", "Vasant Panchami (Saraswati Puja)", 2028, 1, 31, EventCategory.HINDU_FESTIVAL, "Day honoring Maa Saraswati, learning, and creative arts.", traditionOrRegion = "Pan-India")
        registerYearEvent("2028_ratha_saptami", "Ratha Saptami (Surya Jayanti)", 2028, 2, 2, EventCategory.HINDU_FESTIVAL, "Auspicious Magha Shukla Saptami invoking Lord Surya turning his celestial chariot toward the north.", traditionOrRegion = "Pan-India")
        registerYearEvent("2028_thaipusam", "Thai Poosam (Thaipusam)", 2028, 2, 9, EventCategory.HINDU_FESTIVAL, "Tamil festival honoring Lord Murugan with kavadi offerings and devotional penance on Thai Pusam.", traditionOrRegion = "Tamil Nadu / South India")
        registerYearEvent("2028_magha_purnima", "Magha Purnima", 2028, 2, 10, EventCategory.HINDU_FESTIVAL, "Sacred Maghi Purnima bath concluding the month of Magha at Triveni Sangam Prayagraj.", traditionOrRegion = "Pan-India")
        registerYearEvent("2028_shab_e_qadr", "Laylat al-Qadr (Shab-e-Qadr)", 2028, 2, 23, EventCategory.RELIGIOUS_FESTIVAL, "The Night of Decree and Power in the Islamic calendar, commemorating when the Holy Quran was revealed.", traditionOrRegion = "Islamic")
        registerYearEvent("2028_shivaratri", "Maha Shivaratri", 2028, 2, 24, EventCategory.HINDU_FESTIVAL, "Great night of Lord Shiva observed with fasting and meditation.", traditionOrRegion = "Pan-India")
        registerYearEvent("2028_eid_fitr", "Eid al-Fitr", 2028, 2, 27, EventCategory.RELIGIOUS_FESTIVAL, "Concluding Ramadan with community prayers and feasts.", traditionOrRegion = "Islamic")
        registerYearEvent("2028_ash_wed", "Ash Wednesday", 2028, 3, 1, EventCategory.RELIGIOUS_FESTIVAL, "Start of the Christian season of Lent.", traditionOrRegion = "Christian")
        registerYearEvent("2028_holika", "Holika Dahan", 2028, 3, 11, EventCategory.HINDU_FESTIVAL, "Traditional bonfire celebrating the victory of devotion.", traditionOrRegion = "Pan-India")
        registerYearEvent("2028_holi", "Holi (Festival of Colors)", 2028, 3, 12, EventCategory.HINDU_FESTIVAL, "Joyous springtime festival with colors and sweets.", traditionOrRegion = "Pan-India")
        registerYearEvent("2028_hola_mohalla", "Hola Mohalla", 2028, 3, 13, EventCategory.CULTURAL_EVENT, "Sikh martial festival founded by Guru Gobind Singh Ji featuring Gatka, horse riding, and poetry at Anandpur Sahib.", traditionOrRegion = "Punjab / Sikh")
        registerYearEvent("2028_sheetala_ashtami", "Sheetala Ashtami (Basoda)", 2028, 3, 18, EventCategory.HINDU_FESTIVAL, "Prayers to Goddess Sheetala for health and protection, offering Basoda.", traditionOrRegion = "North / West India")
        registerYearEvent("2028_ugadi", "Ugadi / Gudi Padwa / Cheti Chand", 2028, 3, 27, EventCategory.HINDU_FESTIVAL, "Traditional lunisolar new year celebrations across Deccan and Western India (also observed as Navreh / Sajibu Cheiraoba).", traditionOrRegion = "South / West India")
        registerYearEvent("2028_gangaur", "Gangaur", 2028, 3, 30, EventCategory.HINDU_FESTIVAL, "Spring festival of Shiva-Gauri worship celebrated with traditional devotion, songs, and ghewar sweets.", traditionOrRegion = "North / West India")
        registerYearEvent("2028_ram_navami", "Rama Navami", 2028, 4, 4, EventCategory.HINDU_FESTIVAL, "Auspicious birth celebration of Lord Rama.", traditionOrRegion = "Pan-India")
        registerYearEvent("2028_mahavir", "Mahavir Jayanti", 2028, 4, 8, EventCategory.RELIGIOUS_FESTIVAL, "Birth celebration of Bhagwan Mahavira, 24th Tirthankara.", traditionOrRegion = "Jain")
        registerYearEvent("2028_hanuman", "Hanuman Jayanti", 2028, 4, 9, EventCategory.HINDU_FESTIVAL, "Birth celebration of Lord Hanuman.", traditionOrRegion = "Pan-India")
        registerYearEvent("2028_vaisakhi", "Vaisakhi / Baisakhi", 2028, 4, 13, EventCategory.CULTURAL_EVENT, "Spring harvest and historic creation of Khalsa.", traditionOrRegion = "North India")
        registerYearEvent("2028_maundy_thursday", "Maundy Thursday", 2028, 4, 13, EventCategory.RELIGIOUS_FESTIVAL, "Christian Holy Week commemoration of the Last Supper of Jesus Christ with his disciples.", traditionOrRegion = "Christian")
        registerYearEvent("2028_good_friday", "Good Friday", 2028, 4, 14, EventCategory.RELIGIOUS_FESTIVAL, "Solemn day commemorating the passion of Jesus Christ.", traditionOrRegion = "Christian")
        registerYearEvent("2028_easter", "Easter Sunday", 2028, 4, 16, EventCategory.RELIGIOUS_FESTIVAL, "Celebration of the resurrection of Jesus Christ.", traditionOrRegion = "Christian")
        registerYearEvent("2028_akshaya", "Akshaya Tritiya", 2028, 4, 27, EventCategory.HINDU_FESTIVAL, "Day of eternal wealth, new ventures, and gold purchases.", traditionOrRegion = "Pan-India")
        registerYearEvent("2028_eid_adha", "Eid al-Adha (Bakrid)", 2028, 5, 5, EventCategory.RELIGIOUS_FESTIVAL, "Feast of Sacrifice celebrating faith, sharing, and charity.", traditionOrRegion = "Islamic")
        registerYearEvent("2028_buddha", "Buddha Purnima (Vesak)", 2028, 5, 8, EventCategory.RELIGIOUS_FESTIVAL, "Sacred day celebrating birth and enlightenment of Lord Buddha.", traditionOrRegion = "Buddhist")
        registerYearEvent("2028_vat_savitri", "Vat Savitri Vrat", 2028, 5, 24, EventCategory.HINDU_FESTIVAL, "Fasting and banyan tree worship by married women for marital longevity.", traditionOrRegion = "North India")
        registerYearEvent("2028_ganga_dussehra", "Ganga Dussehra", 2028, 6, 2, EventCategory.HINDU_FESTIVAL, "Descent of River Ganga upon Earth.", traditionOrRegion = "North India")
        registerYearEvent("2028_muharram", "Muharram (Ashura)", 2028, 6, 3, EventCategory.RELIGIOUS_FESTIVAL, "Solemn day of remembrance for Hazrat Imam Hussain.", traditionOrRegion = "Islamic")
        registerYearEvent("2028_nirjala_ekadashi", "Nirjala Ekadashi", 2028, 6, 4, EventCategory.HINDU_FESTIVAL, "Sacred waterless fast dedicated to Lord Vishnu.", traditionOrRegion = "Pan-India")
        registerYearEvent("2028_vat_purnima", "Vat Purnima", 2028, 6, 8, EventCategory.HINDU_FESTIVAL, "Jyeshtha Purnima banyan tree worship by married women.", traditionOrRegion = "West / South India")
        registerYearEvent("2028_rath_yatra", "Jagannath Ratha Yatra", 2028, 6, 24, EventCategory.HINDU_FESTIVAL, "Chariot festival of Lord Jagannath in Puri.", traditionOrRegion = "East / Pan-India")
        registerYearEvent("2028_devshayani", "Devshayani Ekadashi", 2028, 7, 3, EventCategory.HINDU_FESTIVAL, "Start of the auspicious four-month Chaturmas period.", traditionOrRegion = "Pan-India")
        registerYearEvent("2028_guru_purnima", "Guru Purnima", 2028, 7, 6, EventCategory.HINDU_FESTIVAL, "Day of heartfelt reverence honoring all teachers and gurus.", traditionOrRegion = "Pan-India")
        registerYearEvent("2028_hariyali_amavasya", "Hariyali Amavasya", 2028, 7, 22, EventCategory.HINDU_FESTIVAL, "New moon day of Shravana celebrated with reverence to nature, tree planting, and prayers to Lord Shiva.", traditionOrRegion = "North / Central India")
        registerYearEvent("2028_hariyali_teej", "Hariyali Teej", 2028, 7, 25, EventCategory.HINDU_FESTIVAL, "Monsoon festival of swings and Shiva-Parvati worship.", traditionOrRegion = "North India")
        registerYearEvent("2028_nag_panchami", "Nag Panchami", 2028, 7, 27, EventCategory.HINDU_FESTIVAL, "Traditional day of serpent deity reverence and prayers.", traditionOrRegion = "Pan-India")
        registerYearEvent("2028_milad_nabi", "Milad-un-Nabi (Eid-e-Milad)", 2028, 8, 3, EventCategory.RELIGIOUS_FESTIVAL, "Birth celebration of Prophet Muhammad.", traditionOrRegion = "Islamic")
        registerYearEvent("2028_varalakshmi", "Varalakshmi Vratam", 2028, 8, 4, EventCategory.HINDU_FESTIVAL, "Fast and worship of Goddess Lakshmi.", traditionOrRegion = "South India")
        registerYearEvent("2028_rakhi", "Raksha Bandhan", 2028, 8, 5, EventCategory.HINDU_FESTIVAL, "Sisters tie sacred threads on brothers' wrists.", traditionOrRegion = "Pan-India")
        registerYearEvent("2028_kajari_teej", "Kajari Teej (Badi Teej)", 2028, 8, 9, EventCategory.HINDU_FESTIVAL, "Celebration of Goddess Parvati, swings, and folk songs.", traditionOrRegion = "North / Central India")
        registerYearEvent("2028_janmashtami", "Krishna Janmashtami", 2028, 8, 13, EventCategory.HINDU_FESTIVAL, "Midnight birth celebration of Lord Krishna.", traditionOrRegion = "Pan-India")
        registerYearEvent("2028_parsi_new_year", "Parsi New Year (Shahenshahi)", 2028, 8, 14, EventCategory.CULTURAL_EVENT, "Parsi Shahenshahi Navroz celebration.", traditionOrRegion = "Parsi")
        registerYearEvent("2028_paryushan_start", "Paryushan Parva Begins", 2028, 8, 16, EventCategory.RELIGIOUS_FESTIVAL, "Period of fasting, introspection, and self-restraint.", traditionOrRegion = "Jain")
        registerYearEvent("2028_hartalika_teej", "Hartalika Teej", 2028, 8, 24, EventCategory.HINDU_FESTIVAL, "Fast dedicated to Goddess Parvati and Lord Shiva.", traditionOrRegion = "North / Central India")
        registerYearEvent("2028_ganesh", "Ganesh Chaturthi (Vinayaka Chavithi)", 2028, 8, 24, EventCategory.HINDU_FESTIVAL, "Festival welcoming Lord Ganesha into homes and pandals.", traditionOrRegion = "Pan-India")
        registerYearEvent("2028_samvatsari", "Samvatsari (Michhami Dukkadam)", 2028, 8, 24, EventCategory.RELIGIOUS_FESTIVAL, "Day of universal forgiveness in Jainism.", traditionOrRegion = "Jain")
        registerYearEvent("2028_radha_ashtami", "Radha Ashtami", 2028, 8, 28, EventCategory.HINDU_FESTIVAL, "Appearance day of Shri Radha Rani, celebrated with devotional joy.", traditionOrRegion = "Pan-India")
        registerYearEvent("2028_anant_chaturdashi", "Anant Chaturdashi (Ganesh Visarjan)", 2028, 9, 3, EventCategory.HINDU_FESTIVAL, "Final immersion of Lord Ganesha and Anant Vrata.", traditionOrRegion = "Pan-India")
        registerYearEvent("2028_onam", "Onam (Thiruvonam)", 2028, 9, 4, EventCategory.CULTURAL_EVENT, "Grand finale of Kerala's 10-day harvest festival.", traditionOrRegion = "Kerala")
        registerYearEvent("2028_sarva_pitru", "Sarva Pitru Amavasya (Mahalaya)", 2028, 9, 18, EventCategory.HINDU_FESTIVAL, "Day honoring ancestors and invocation of Maa Durga.", traditionOrRegion = "Pan-India")
        registerYearEvent("2028_navratri", "Shardiya Navratri Begins", 2028, 10, 19, EventCategory.HINDU_FESTIVAL, "Ghatasthapana marking nine auspicious days of Durga worship.", traditionOrRegion = "Pan-India")
        registerYearEvent("2028_durga_ashtami", "Maha Ashtami / Durga Puja", 2028, 10, 26, EventCategory.HINDU_FESTIVAL, "Auspicious day of Durga Puja celebrating supreme divine power.", traditionOrRegion = "East / Pan-India")
        registerYearEvent("2028_bathukamma", "Saddula Bathukamma", 2028, 10, 26, EventCategory.CULTURAL_EVENT, "Culmination of the 9-day Bathukamma festival widely observed across Telangana, honoring Mother Gauri with vibrant floral arrangements.", traditionOrRegion = "Telangana, India")
        registerYearEvent("2028_dussehra", "Dussehra (Vijayadashami)", 2028, 10, 27, EventCategory.HINDU_FESTIVAL, "Victory of good over evil; effigies of Ravana are burned.", traditionOrRegion = "Pan-India")
        registerYearEvent("2028_sharad_purnima", "Sharad Purnima (Kojagari Puja)", 2028, 11, 2, EventCategory.HINDU_FESTIVAL, "Full moon night of Goddess Lakshmi.", traditionOrRegion = "Pan-India")
        registerYearEvent("2028_karwa_chauth", "Karwa Chauth", 2028, 11, 6, EventCategory.HINDU_FESTIVAL, "Fast observed for the longevity and health of life partners.", traditionOrRegion = "North / West India")
        registerYearEvent("2028_kalabhairav_ashtami", "Kalabhairav Ashtami (Bhairava Jayanti)", 2028, 11, 9, EventCategory.HINDU_FESTIVAL, "Appearance day of Lord Kalabhairava, protector deity.", traditionOrRegion = "Pan-India")
        registerYearEvent("2028_ahoi_ashtami", "Ahoi Ashtami", 2028, 11, 9, EventCategory.HINDU_FESTIVAL, "Fast observed by mothers for children's well-being.", traditionOrRegion = "North India")
        registerYearEvent("2028_govatsa", "Govatsa Dwadashi (Vagh Baras)", 2028, 11, 14, EventCategory.HINDU_FESTIVAL, "Honoring cows and maternal affection.", traditionOrRegion = "West / Pan-India")
        registerYearEvent("2028_dhanteras", "Dhanteras", 2028, 11, 15, EventCategory.HINDU_FESTIVAL, "Auspicious day honoring wealth and Lord Dhanvantari.", traditionOrRegion = "Pan-India")
        registerYearEvent("2028_naraka_chaturdashi", "Naraka Chaturdashi (Chhoti Diwali)", 2028, 11, 16, EventCategory.HINDU_FESTIVAL, "Day before Diwali celebrating the defeat of Narakasura.", traditionOrRegion = "Pan-India")
        registerYearEvent("2028_diwali", "Diwali (Deepavali - Lakshmi Puja)", 2028, 11, 17, EventCategory.HINDU_FESTIVAL, "Grand festival of lights and home illuminations.", traditionOrRegion = "Pan-India")
        registerYearEvent("2028_bandi_chhor", "Bandi Chhor Divas", 2028, 11, 17, EventCategory.RELIGIOUS_FESTIVAL, "Sikh celebration commemorating the historic release of Guru Hargobind Ji and 52 kings from Gwalior Fort.", traditionOrRegion = "Sikh")
        registerYearEvent("2028_mahavir_nirvana", "Mahavira Nirvana (Jain Diwali)", 2028, 11, 17, EventCategory.RELIGIOUS_FESTIVAL, "Jain festival celebrating the attainment of Moksha by Bhagwan Mahavira with sacred lamps (Divo).", traditionOrRegion = "Jain")
        registerYearEvent("2028_govardhan", "Govardhan Puja / Annakut", 2028, 11, 18, EventCategory.HINDU_FESTIVAL, "Expressing gratitude to nature and Lord Krishna's protection.", traditionOrRegion = "Pan-India")
        registerYearEvent("2028_bhai_dooj", "Bhai Dooj", 2028, 11, 19, EventCategory.HINDU_FESTIVAL, "Celebration of sibling affection and blessings.", traditionOrRegion = "Pan-India")
        registerYearEvent("2028_chhath_nahay_khay", "Chhath Puja — Nahay Khay", 2028, 11, 21, EventCategory.HINDU_FESTIVAL, "First day of Chhath Puja: holy purification dip and satvik meal.", traditionOrRegion = "North / East India")
        registerYearEvent("2028_chhath_kharna", "Chhath Puja — Kharna / Lohanda", 2028, 11, 22, EventCategory.HINDU_FESTIVAL, "Second day of Chhath: fast concluded with gur kheer prasad.", traditionOrRegion = "North / East India")
        registerYearEvent("2028_skanda_sashti", "Skanda Sashti (Soorasamharam)", 2028, 11, 22, EventCategory.HINDU_FESTIVAL, "Six-day festival celebrating Lord Murugan's victory over the demon Soorapadman in Tiruchendur and temples worldwide.", traditionOrRegion = "Tamil Nadu / South India")
        registerYearEvent("2028_chhath", "Chhath Puja — Sandhya Arghya", 2028, 11, 23, EventCategory.HINDU_FESTIVAL, "Third day of Chhath: sacred evening offerings to Surya at river ghats.", traditionOrRegion = "North / East India")
        registerYearEvent("2028_chhath_usha_arghya", "Chhath Puja — Usha Arghya / Parana", 2028, 11, 24, EventCategory.HINDU_FESTIVAL, "Fourth day of Chhath: morning offerings to rising Surya concluding the fast.", traditionOrRegion = "North / East India")
        registerYearEvent("2028_akshaya_navami", "Akshaya Navami (Amla Navami)", 2028, 11, 26, EventCategory.HINDU_FESTIVAL, "Auspicious day dedicated to worshipping the sacred Amla tree and charity.", traditionOrRegion = "Pan-India")
        registerYearEvent("2028_devutthana", "Devutthana Ekadashi", 2028, 11, 28, EventCategory.HINDU_FESTIVAL, "Awakening of Lord Vishnu concluding Chaturmas.", traditionOrRegion = "Pan-India")
        registerYearEvent("2028_gita_jayanti", "Gita Jayanti / Mokshada & Vaikuntha Ekadashi", 2028, 11, 28, EventCategory.HINDU_FESTIVAL, "Anniversary of the revelation of the Bhagavad Gita and Vaikuntha Dwaram opening.", traditionOrRegion = "Pan-India")
        registerYearEvent("2028_tulsi_vivah", "Tulsi Vivah", 2028, 11, 29, EventCategory.HINDU_FESTIVAL, "Ceremonial marriage of the Tulsi plant to Shaligram.", traditionOrRegion = "Pan-India")
        registerYearEvent("2028_karthigai_deepam", "Karthigai Deepam", 2028, 12, 1, EventCategory.HINDU_FESTIVAL, "Ancient Tamil festival of lights celebrating the cosmic pillar of fire of Lord Shiva at Tiruvannamalai.", traditionOrRegion = "Tamil Nadu / South India")
        registerYearEvent("2028_tegh_bahadur", "Martyrdom Day of Guru Tegh Bahadur Ji", 2028, 12, 1, EventCategory.RELIGIOUS_FESTIVAL, "Solemn historical remembrance honoring the 9th Sikh Guru.", traditionOrRegion = "Sikh")
        registerYearEvent("2028_dev_deepawali", "Dev Deepawali", 2028, 12, 1, EventCategory.HINDU_FESTIVAL, "Festival of lights of the gods celebrated at the ghats of Varanasi on Kartik Purnima eve.", traditionOrRegion = "North India")
        registerYearEvent("2028_kartik_purnima", "Kartik Purnima", 2028, 12, 2, EventCategory.HINDU_FESTIVAL, "Sacred full moon of Kartik month observed with holy river baths, lamps, and charity.", traditionOrRegion = "Pan-India")
        registerYearEvent("2028_guru_nanak", "Guru Nanak Gurpurab", 2028, 12, 2, EventCategory.RELIGIOUS_FESTIVAL, "Prakash Utsav honoring Guru Nanak Dev Ji.", traditionOrRegion = "Sikh")
        registerYearEvent("2028_dattatreya", "Dattatreya Jayanti", 2028, 12, 31, EventCategory.HINDU_FESTIVAL, "Appearance day of Lord Dattatreya, divine incarnation of Brahma, Vishnu, and Shiva on Margashirsha Purnima.", traditionOrRegion = "Pan-India")
    }
}

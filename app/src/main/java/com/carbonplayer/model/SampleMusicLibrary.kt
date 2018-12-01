package com.carbonplayer.model

import com.carbonplayer.model.entity.Album
import com.carbonplayer.model.entity.skyjam.SkyjamTrack

object SampleMusicLibrary {

    fun albums(): List<Album> {
        return listOf(
                Album(Album.SAMPLE, true, "", null,
                        "2753", "Blake",
                        "", 2018, "Electronic",
                        "https://lh3.googleusercontent.com/FL9C8t0fI0w8OcFR6oowOnjWgGMeJdrrnFYHm3745kpOPzo8HmT78XHmQIS3hUuWwSDNm1y" +
                                "YZNUyj8MepwGujGKy8pqKGRtuSd3Hj0rvYX52UbEx7vtzDDtvtUJiV9Z67y1CWj5NnRuiKVNx-Pec0JBy2peFtQgjAuOF-_i44EpvFnX7UfSg2tp_T2zDemhP2NQkz4b6_VuddP" +
                                "fsD_Ad8uiWb6PsvMFzfTR3T0N7jE-Nt2hp1HVLxD0lBfLwc8VOZg5ALdA_QdVZaPIXgGU8MgM5r0ABvLUTSbSyIhf-8xlibmWSd34R7RefFswt4_btqK5Mw_XfXDuOozuMDQbtGDidCQl41" +
                                "KUAHoKNPwfb1NycAAIBo8eDU7rY7maCI0c7eIgrfjUpdh2qwPVzaQMTU6k1XdxRLrvLXSYdbCbLQxyRfFgALJ_-zXgib157bA1LfUo_jkny_t7pUGsnmZEKUjpaaSEAIgDlqfBSe92RUPrlze" +
                                "JhQJsMPqQv6UiUz8TxyO3nqqaJNkDYDGUgpKZK7wDlTvwuxnfOTL0tA24Px-tvzXrmq0yvnCkhc38rIjpJFDFPIhfNcA_0-Yi0QmKtvQcMDtwS-kyCddi5js8X=s1732-no"
                ),
                Album(Album.SAMPLE, true, "", null,
                        "Archea", "Defenders of Saturn",
                        "", 2005, "Hip-Hop",
                        "https://lh3.googleusercontent.com/4aZHUwrcn9FVhPZ-RuS1za4QamOngIY3ndPgcGTYvA2WO1mM3WcfVLJ_Lv1Mm97KXkrJjTuNkHuhlaGRqmMH_ouTWxXfnp2KOWxAMKzl08hRH0lt8_hKuGxTdW4dRK" +
                                "XmDH68aWlNQo2Sj6K9SUFrFSpyFMjGmB1V-BowIQeLaYu_nTTe7G1S1PNAlH6TyUTEBDBZl5cRT0J_FVXgtNnHANyvMtfo_RKRz2eMpbQCLXyVz8DhCZHf2FHv9A4vJybgE_pd6B_JP4joBb_WJGwDkeefHc5fSFI" +
                                "3nc4EucYo5rNgincDJm1T3kHRWzVcq165f4fMYHlmiM4s118fNJ9vA5m2uPmzYWC64tMEkARjRgbOO-8t5xDUOmbUcIWPgPDZm0ZKs7j-gvx8Kkju5Y1fW_FpahIfiE3gGpC2aZoJbghZX2N7uLvB0bVE7zTrX_" +
                                "XUhHh6S9BdFJ-8iLnXqxjb3dwVjmqDDzpXwbCBYJjxGmYiD-rdRq9S2X3yYOSAPiEQvX1HEHxfviqJzmuknlgKZ-dQwbCHtWvO8yl3UIH1AXna2E9AH37aEKDNS3x-RmNynyzV32tOpKnxRyM7fuWbHJD_FqrH3gt_lU_R_Qpg=s804-no"),
                Album(Album.SAMPLE, true, "", null,
                        "In Mediums", "Bellevue",
                        "", 2007, "Indie",
                        "https://lh3.googleusercontent.com/bt2niRoK0GOoKUTsB7KBDVAyxpX-du7udK8ZYegBw_wBGO71W11eJCslSi8_TzK4eSh-DmkqENCQUyCMgNmXft2006FoyqHWkZP71Ev5heXtFa283skPM_" +
                                "yUH1CV_KVPizwiYW2FgYtpwyZkhVuAhwrAH5yaviE8QNg3WUhQuIY5H-w0P_qB-YNlrEJRZWdaOK6Dbmdr1qhxSFBkPBHVI_2XL80iCmNeXVbKgSPh8VxoH_gbcQO_bXbCQNdwfi_LVSJ9wVxbG52bNjMyWpDD" +
                                "iPXxasKWb9S5WXl6n5o-p3cshAl5fNre_-BCY_0YJwV2HJ1kUow5l0iJZ2gBXb6Q9eafWIUrt22BPhZ7D45Wg3y63Dr2d48NWcQv_DK6BciN60uwAnjS27g5yM0RNHkxARgIiMTP0SILBGujFxFIAZSj-dwJDFNhm" +
                                "UU8YeWEDorOcKNaovV1EUqSlNTJhOsyIg70tpg4ZCMVVTpk0XMKZAqBwSSYUU3mvrJFQZaSAeRMnruNhz6zTnIJ1fnb0vXRqvxE4MxBL_5EaJyfTe713j3PacG-4GO7rIdIJFz1NfSdNUb3Za0iJse0cH2lrhswjDaY" +
                                "YhubEmj2yoKtvJi0=w1080-h1079-no"),
                Album(Album.SAMPLE, true, "", null,
                        "Today", "Point Four",
                        "", 2011, "Pop",
                        "https://lh3.googleusercontent.com/pEVVJckVeTWazrz-HQTvK5J0VVpVgOVUEdg8w8FLjAWNkp2fqkuAkSFzqY02rreXjsBeIb0Eq5l3_lkZJcOjDoMTpevCkceMMOSZKhQQU-tJpkZ8dJ2ew" +
                                "ECdN6P79BtUISUNNa79yTOGwuhCu4fiihz5zLd4iCQDD0EC2RwsnkKenFpt5oOecJl4F92mVu-y5JlFCeRrJNuZqKCf0Y44RU5tq8mmej5WMdDXynH0n244gL_VM-oRp-IykPpsfjFnTt1I7QZUCrARwbfRc" +
                                "XTjBHp_9kGf-2As9t3Y8t7tpeo9NiLUdI3t_fkfFZJw0CBF2yQd-GWhKVPlN6K8st3RhbRT7dpws1eRqQkQCl9NZTgCkFjkMTEa2IW65JaATI2-3bBg6_M_0nuK_iSQkjifk8T8HxoQDDK3NtWkJs17UydLPE8" +
                                "RwNcWGXYOGVBwMo9FEc8T6e6xFQ3Gpq8hOOlbeSnOU3cVj7_dp1ugrlvPR5hz73smi-3-Kb6-RY0sKMcx9e6Medy7jKSR4JdKK3TsMX76wS6312zEXrBBtTfjOXe312_QD4-lJZFTW1BWbiu-C_p-GJaSFMXu" +
                                "YEHPVVrwbuCKxY79ugwJyr-xk00t=s1062-no"),
                Album(Album.SAMPLE, true, "", null,
                        "Under Duress", "Indigo",
                        "", 2013, "Rock",
                        "https://lh3.googleusercontent.com/okraJMPVXyVCHnjKaH2QfTSniWtP0ty4piP9IO5XdrVndBj-NAUJd-ov1wC4GRvrHwSpLQWPmUnWXs70geYEOzbo7EKLuFyeIooX5DenAGGOUpa4-ZBkz" +
                                "92TMWTNAz38_u1fl7Ci51SbOG1i1YcFXaJhB9qjGnxvzFiv-OqotBxs9C-sLIgsoKVsC31JLu8IViO97yyRzBn5D5c46HGBekSj6sQZejD08McjIrQAU-o-9dFKs-F8kSO5i4U94DU_CrpZWtPknNb4MRi9G" +
                                "BtYDJt3Fr8DPUIn4KOAQ5zmh673CZPmB9YtM4T-y1c8rt7nlY5IwMhHlvn2p2QsR2c1C3y3Oq9HYpdIGmjz9TaWzLJOT7sjX8ceOYTLf-Y4YZDx3mwvDyHTs_SMuOGohEFavySp8YNef1X5l7YQmWuOXXz7j" +
                                "SW-MyWgu7YTvKzzjzzp2os-y6KJOJaBU0FsuwSiG9Hpw7n0EcDrjh6PgYDY0nKGE--6uqX7rT162myGPSDGvcUEs4WTT2HG0BQjecx1ljotp63clNTH8d3M4X3pHiVLKcINs-uxn3hMC4iUXxSqfJP8RBb3ewoaf" +
                                "E7d-Nt_SQd9ap3e8VMQq7pz3zjw=w245-h244-no"),
                Album(Album.SAMPLE, true, "", null,
                        "Travelers", "Caracal",
                        "", 2013, "Rock",
                        "https://lh3.googleusercontent.com/9NDte0EsMo0vciJzo39lhrG_HqKwfHXjiMXaL1t391yc1J3NRzlXEdnysJCoY_oG1dbrEaQaRV-s5TxMkhc313oNbYi9LiqJFHTgup0PPvsdx" +
                                "oZTZuh0MIqq7gBP7z2CoG3GGSIuO_GmcuV-5cGIgShmPNOUrT-g9oOdHmTr1rozI4TKMqRebe7lmJGViq2MrJfuEnftu6965CpJTfIWVmV0qWttQWubd-6Ihp7TajwOG--fmNfuH0o9kAdOJ" +
                                "JWit_TjEr5F-vVaboA_Bf_NgoScQ8biXi3iwoH9O95G9a7JuUcmkhngRinxuVKYey9ygkLuMthJ3Ati4RrbUa-lP-ShP6ereCFTlYICigzZCsSTpqLJBfFj4Pq-HM5lHcg-sQMTtjrNSWQjTEJ" +
                                "5fJydA2E9y80rKccCljg-Ko7uyYHhGxwsjkNM6uitZGEKVcVS2HRMaQiu8fMwmqac7xicIaQMyj4L_AofoIxKZ52LRRuRudAPG3Xw-DaJpdQFpWb_9EDUGkGAORSZ6i5HR_60ZGwfbJV68eakZ" +
                                "DMekaM0wmlP0QLrKD_HvJr11KMcWqz9fWS0HOQgYI1xPI79sXKr67g5JeRAoBhEFdWDr1PF=w1440-h1439-no")
        )
    }

    fun tracks(artist: String, album: String): List<SkyjamTrack> {
        return listOf(
                SkyjamTrack.sample("Intro", 1, artist, album),
                SkyjamTrack.sample("Fire Eyes", 2, artist, album),
                SkyjamTrack.sample("Remedial", 3, artist, album),
                SkyjamTrack.sample("Tapestry", 4, artist, album),
                SkyjamTrack.sample("Saltwater (Interlude)", 5, artist, album),
                SkyjamTrack.sample("Virus", 6, artist, album),
                SkyjamTrack.sample("Devil", 7, artist, album)
        )
    }
}
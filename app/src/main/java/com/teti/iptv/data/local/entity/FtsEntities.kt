package com.teti.iptv.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4

/*
 * Tam metin arama dizinleri. Ana tabloların içeriğini (contentEntity) yansıtır; Room
 * ekleme/silme/güncellemede tetikleyicilerle senkron tutar. LIKE '%…%' taraması yerine
 * dizin kullanıldığı için on binlerce satırda arama anlıktır.
 */

@Fts4(contentEntity = ChannelEntity::class)
@Entity(tableName = "channels_fts")
data class ChannelFts(val name: String, val categoryName: String)

@Fts4(contentEntity = VodEntity::class)
@Entity(tableName = "vod_fts")
data class VodFts(val title: String, val categoryName: String)

@Fts4(contentEntity = SeriesEntity::class)
@Entity(tableName = "series_fts")
data class SeriesFts(val title: String, val categoryName: String)

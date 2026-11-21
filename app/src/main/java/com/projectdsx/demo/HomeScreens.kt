package com.projectdsx.demo

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage

// ---------- Piezas comunes ----------

@Composable
fun GameCard(
    g: Game, w: Dp, h: Dp, accent: Color,
    shape: Shape = RoundedCornerShape(6.dp),
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current
    val app = LocalApp.current
    val w = w * app.iconScale
    val h = h * app.iconScale
    val scale by animateFloatAsState(if (focused) 1.08f else 1f, label = "scale")
    Box(
        Modifier.size(w, h).scale(scale).clip(shape)
            .border(if (focused) 3.dp else 0.dp, accent, shape)
            .background(Color(0xFF262626))
            .onFocusChanged {
                if (it.isFocused && !focused) Sfx.nav(view, app)
                focused = it.isFocused
            }
            .clickable(onClick = onClick)
    ) {
        if (g.steamAppId != null) {
            AsyncImage(Steam.cover(g.steamAppId), g.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else g.pkg?.let { p ->
            val bmp = remember(p) {
                runCatching { ctx.packageManager.getApplicationIcon(p).toBitmap(256, 256).asImageBitmap() }.getOrNull()
            }
            bmp?.let { Image(it, g.title, Modifier.fillMaxSize().padding(18.dp)) }
        }
        Text(
            g.title, color = Color.White, fontSize = 12.sp, maxLines = 1,
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
                .background(Color(0x99000000)).padding(horizontal = 6.dp, vertical = 3.dp)
        )
        if (g.gfnId != null) {
            Text(
                "GFN", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                    .background(Color(0xFF76B900), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 1.dp)
            )
        }
    }
}

@Composable
fun Wallpaper(pkg: String?, brush: Brush, imageAlpha: Float = 0.45f) {
    val ctx = LocalContext.current
    Box(Modifier.fillMaxSize().background(brush)) {
        pkg?.let { p ->
            val bmp = remember(p) {
                runCatching { ctx.packageManager.getApplicationIcon(p).toBitmap(512, 512).asImageBitmap() }.getOrNull()
            }
            bmp?.let {
                Image(
                    it, null, Modifier.fillMaxSize().blur(36.dp),
                    contentScale = ContentScale.Crop, alpha = imageAlpha
                )
            }
        }
    }
}

@Composable
fun SettingsBtn(color: Color, onSettings: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        "⚙  Ajustes", color = color, fontSize = 16.sp,
        modifier = modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onSettings).padding(12.dp)
    )
}

// ---------- Selector de menú ----------

@Composable
fun Home(c: Console, games: List<Game>, wallpaper: String?, onOpen: (Game) -> Unit, onProfile: () -> Unit, onSettings: () -> Unit) {
    Box(Modifier.fillMaxSize()) { HomeInner(c, games, wallpaper, onOpen, onProfile, onSettings)
        if (c != Console.XONE) StatusWidget(Modifier.align(Alignment.BottomEnd).padding(14.dp), onProfile) }
}

@Composable
private fun HomeInner(c: Console, games: List<Game>, wallpaper: String?, onOpen: (Game) -> Unit, onProfile: () -> Unit, onSettings: () -> Unit) {
    when (c) {
        Console.X360 -> X360Menu(games, wallpaper, onOpen, onSettings)
        Console.XONE -> XOneMenu(games, wallpaper, onOpen, onProfile, onSettings)
        Console.XSERIES -> XSeriesMenu(games, wallpaper, onOpen, onSettings)
        Console.PS4 -> Ps4Menu(games, wallpaper, onOpen, onSettings)
        Console.PS5 -> Ps5Menu(games, wallpaper, onOpen, onSettings)
        Console.DECK -> DeckMenu(games, wallpaper, onOpen, onSettings)
    }
}

// ---------- Xbox 360: "blades" claros con verde ----------
@Composable
fun X360Menu(games: List<Game>, wp: String?, open: (Game) -> Unit, settings: () -> Unit) {
    val green = Color(0xFF7CB82F)
    Wallpaper(wp, Brush.verticalGradient(listOf(Color(0xFFF4F4F4), Color(0xFFBBD890))), 0.25f)
    Column(Modifier.fillMaxSize().padding(28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            listOf("Mercado", "Juegos", "Medios", "Sistema").forEachIndexed { i, t ->
                Text(
                    t, fontSize = if (i == 1) 26.sp else 20.sp,
                    color = if (i == 1) green else Color(0xFF666666),
                    fontWeight = if (i == 1) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(end = 30.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            Text("XBOX 360", color = green, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(36.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(8.dp)) {
            items(games, key = { it.id }) { GameCard(it, 190.dp, 120.dp, green) { open(it) } }
        }
        Spacer(Modifier.weight(1f))
        SettingsBtn(Color(0xFF444444), settings)
    }
}

// ---------- Xbox Series: filas limpias y oscuras ----------
@Composable
fun XSeriesMenu(games: List<Game>, wp: String?, open: (Game) -> Unit, settings: () -> Unit) {
    val green = Color(0xFF107C10)
    Wallpaper(wp, Brush.verticalGradient(listOf(Color(0xFF0E0E0E), Color(0xFF1A2A1A))), 0.4f)
    Column(Modifier.fillMaxSize().padding(32.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            listOf("Inicio", "Mi biblioteca", "Game Pass", "Tienda").forEachIndexed { i, t ->
                Text(
                    t, color = if (i == 0) Color.White else Color(0xFF9A9A9A), fontSize = 18.sp,
                    modifier = Modifier.padding(end = 26.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(34.dp).background(green, CircleShape))
        }
        Spacer(Modifier.height(30.dp))
        Text("Jugados recientemente", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(8.dp)) {
            items(games, key = { it.id }) { GameCard(it, 230.dp, 140.dp, Color.White, RoundedCornerShape(10.dp)) { open(it) } }
        }
        Spacer(Modifier.weight(1f))
        SettingsBtn(Color.White, settings)
    }
}

// ---------- PS4: fila de iconos sobre azul ----------
@Composable
fun Ps4Menu(games: List<Game>, wp: String?, open: (Game) -> Unit, settings: () -> Unit) {
    Wallpaper(wp, Brush.verticalGradient(listOf(Color(0xFF0A2A6E), Color(0xFF051438))), 0.35f)
    Column(Modifier.fillMaxSize().padding(28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            listOf("Notificaciones", "Tienda", "Biblioteca").forEach {
                Text(it, color = Color(0xFFB8C7E8), fontSize = 16.sp, modifier = Modifier.padding(end = 24.dp))
            }
            Spacer(Modifier.weight(1f))
            Text("PS4", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        }
        Spacer(Modifier.height(60.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(10.dp)) {
            items(games, key = { it.id }) { GameCard(it, 150.dp, 150.dp, Color.White, RoundedCornerShape(2.dp)) { open(it) } }
        }
        Spacer(Modifier.weight(1f))
        SettingsBtn(Color(0xFFB8C7E8), settings)
    }
}

// ---------- PS5: pestañas arriba y tarjetas redondeadas centradas ----------
@Composable
fun Ps5Menu(games: List<Game>, wp: String?, open: (Game) -> Unit, settings: () -> Unit) {
    Wallpaper(wp, Brush.radialGradient(listOf(Color(0xFF1E2A55), Color(0xFF07070F))), 0.5f)
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Juegos", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Medios", color = Color(0xFF8A8FA8), fontSize = 22.sp)
        }
        Spacer(Modifier.weight(1f))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(horizontal = 40.dp, vertical = 12.dp)
        ) {
            items(games, key = { it.id }) { GameCard(it, 170.dp, 170.dp, Color.White, RoundedCornerShape(22.dp)) { open(it) } }
        }
        Spacer(Modifier.weight(1f))
        SettingsBtn(Color(0xFFB0B5D0), settings)
    }
}

// ---------- Steam Deck: menú lateral + cuadrícula ----------
@Composable
fun DeckMenu(games: List<Game>, wp: String?, open: (Game) -> Unit, settings: () -> Unit) {
    val blue = Color(0xFF1A9FFF)
    Wallpaper(wp, Brush.verticalGradient(listOf(Color(0xFF1B2838), Color(0xFF0E141B))), 0.3f)
    Row(Modifier.fillMaxSize()) {
        Column(Modifier.width(190.dp).fillMaxHeight().background(Color(0xCC0E141B)).padding(20.dp)) {
            Text("STEAM DECK", color = blue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(24.dp))
            listOf("Biblioteca", "Tienda", "Comunidad", "Descargas").forEach {
                Text(it, color = Color(0xFFC7D5E0), fontSize = 17.sp, modifier = Modifier.padding(vertical = 9.dp))
            }
            Spacer(Modifier.weight(1f))
            SettingsBtn(blue, settings, Modifier.offset(x = (-12).dp))
        }
        LazyVerticalGrid(
            GridCells.Adaptive(170.dp), Modifier.weight(1f).fillMaxHeight(),
            contentPadding = PaddingValues(20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            gridItems(games, key = { it.id }) { GameCard(it, 170.dp, 100.dp, blue, RoundedCornerShape(4.dp)) { open(it) } }
        }
    }
}

package com.example.noya.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.noya.ui.ResponsiveDimens

@Composable
fun LargeAccessibleButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    backgroundColor: Color = Color(0xFF5DADE2), // Azul claro y suave por defecto
    modifier: Modifier = Modifier,
    customHeight: Dp? = null,
    customIconSize: Dp? = null,
    customTextSize: Float? = null
) {
    // Dimensiones responsivas
    val largeButtonHeight = ResponsiveDimens.current.largeButtonHeight
    val largeIconSize = ResponsiveDimens.current.largeIconSize
    val mediumPadding = ResponsiveDimens.current.mediumPadding
    val mediumTextSize = ResponsiveDimens.current.mediumTextSize
    val buttonHeight = customHeight ?: largeButtonHeight
    val iconSize = customIconSize ?: largeIconSize
    val textSize = customTextSize ?: mediumTextSize

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(buttonHeight),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(iconSize),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(mediumPadding))
            Text(
                text = text,
                fontSize = textSize.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun GridImageButton(
    text: String,
    @DrawableRes imageRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF5DADE2)
) {
    // Dimensiones responsivas
    val cornerRadius = ResponsiveDimens.current.cornerRadius
    val smallPadding = ResponsiveDimens.current.smallPadding
    val gridImageSize = ResponsiveDimens.current.gridImageSize
    val smallTextSize = ResponsiveDimens.current.smallTextSize

    Button(
        onClick = onClick,
        modifier = modifier
            .aspectRatio(1f), // Hace el botón cuadrado
        shape = androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius), // Esquinas redondeadas, no circular
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp
        ),
        contentPadding = PaddingValues(smallPadding)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = text,
                modifier = Modifier.size(gridImageSize)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                fontSize = smallTextSize.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun NumericKeyboard(
    onNumberClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    customButtonHeight: Dp? = null,
    customTextSize: Float? = null
) {
    // Dimensiones responsivas
    val smallPadding = ResponsiveDimens.current.smallPadding
    val smallButtonHeight = ResponsiveDimens.current.smallButtonHeight
    val largeTextSize = ResponsiveDimens.current.largeTextSize
    val smallIconSize = ResponsiveDimens.current.smallIconSize

    val buttonHeight = customButtonHeight ?: smallButtonHeight
    val textSize = customTextSize ?: largeTextSize

    val buttons = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("*", "0", "#")
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(smallPadding)
    ) {
        buttons.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(smallPadding)
            ) {
                row.forEach { digit ->
                    Button(
                        onClick = { onNumberClick(digit) },
                        modifier = Modifier
                            .weight(1f)
                            .height(buttonHeight),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF34495E), // Azul grisáceo oscuro pero suave
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = digit,
                            fontSize = textSize.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Fila para el botón de borrar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = onBackspaceClick,
                modifier = Modifier
                    .weight(1f)
                    .height(buttonHeight),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE67E22), // Naranja más suave
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Borrar",
                    modifier = Modifier.size(smallIconSize)
                )
            }
        }
    }
}

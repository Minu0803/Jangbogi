package com.minwoo.jangbogi.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.minwoo.jangbogi.domain.Suggestion

@Composable
fun SuggestionChips(
    suggestions: List<Suggestion>,
    onPick: (Suggestion) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(suggestions, key = { "${it.category.name}-${it.name}" }) { suggestion ->
            SuggestionChip(
                onClick = { onPick(suggestion) },
                shape = RoundedCornerShape(50),
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                border = null,
                label = {
                    Text(
                        text = "${suggestion.category.emoji} ${suggestion.name}",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            )
        }
    }
}

import re

with open("composeApp/src/androidMain/kotlin/com/maxrave/simpmusic/ui/component/LiquidGlassAppBottomNavigationBar.android.kt", "r") as f:
    content = f.read()

# Make sure imports are present exactly like this
imports_to_add = """
import com.maxrave.simpmusic.ui.ext.hapticClickable
import androidx.compose.material3.Icon
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.Mic
"""

if "import com.maxrave.simpmusic.ui.ext.hapticClickable" not in content:
    content = content.replace("import com.maxrave.simpmusic.expect.ui.hapticClickable", "import com.maxrave.simpmusic.ui.ext.hapticClickable")

if "import androidx.compose.material3.Icon" not in content:
    content = content.replace("import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi", "import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi\nimport androidx.compose.material3.Icon\nimport com.maxrave.simpmusic.ui.icon.SimpIcons\nimport com.maxrave.simpmusic.ui.icon.Mic")


# replace method calls if they are not already changed
content = content.replace("Icon(com.maxrave.simpmusic.ui.icon.SimpIcons.Mic", "androidx.compose.material3.Icon(com.maxrave.simpmusic.ui.icon.SimpIcons.Mic")


with open("composeApp/src/androidMain/kotlin/com/maxrave/simpmusic/ui/component/LiquidGlassAppBottomNavigationBar.android.kt", "w") as f:
    f.write(content)

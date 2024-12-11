import com.kengine.GameRunner
import com.kengine.createGameContext
import imageshuffle.ImageShuffleGame


fun main() {
    createGameContext(
        title = "Kengine - Image Shuffle",
        width = 800,
        height = 800,
    ) {
        GameRunner(frameRate = 60) {
            ImageShuffleGame()
        }
    }
}


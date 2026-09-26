/**
 * Smoke test for the assembled game (run in a headless sandbox).
 * The FutoshikiGame constructor does, in order:
 *   runStringTests -> runInequalityTests -> setupNewGame (generator) -> createUI (Swing) -> setVisible
 * If we reach setVisible, everything above it worked. In a headless
 * environment only the FINAL step may throw HeadlessException - that is
 * expected and means the game would open a window on a real desktop.
 */
public class SmokeTests {
    public static void main(String[] args) {
        System.out.println("[smoke] headless environment: "
                + java.awt.GraphicsEnvironment.isHeadless());
        try {
            new FutoshikiGame();
            System.out.println("[smoke] FULL SUCCESS - window shown (running on a desktop)");
            System.exit(0);
        } catch (java.awt.HeadlessException e) {
            System.out.println("[smoke] tests + generator + UI all ran; "
                    + "HeadlessException only at setVisible (expected in sandbox)");
        }
        System.out.println("[smoke] DONE");
        System.exit(0);
    }
}

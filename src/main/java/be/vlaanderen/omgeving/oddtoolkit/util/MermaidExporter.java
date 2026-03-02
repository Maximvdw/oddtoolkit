package be.vlaanderen.omgeving.oddtoolkit.util;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class to export Mermaid diagrams to PNG format with high resolution.
 * Uses Playwright to render the diagram via a headless browser.
 */
public class MermaidExporter {

  private static final String MERMAID_CDN = "https://cdn.jsdelivr.net/npm/mermaid/dist/mermaid.min.js";
  private static final int DEFAULT_SCALE = 10;

  /**
   * Exports a Mermaid diagram to a high-resolution PNG file.
   *
   * @param mermaidContent the Mermaid diagram content (as string)
   * @param outputPath the output PNG file path
   * @throws IOException if file operations fail
   */
  public static void exportToPng(String mermaidContent, String outputPath) throws IOException {
    exportToPng(mermaidContent, outputPath, DEFAULT_SCALE);
  }

  /**
   * Exports a Mermaid diagram to a PNG file with specified scale.
   *
   * @param mermaidContent the Mermaid diagram content (as string)
   * @param outputPath the output PNG file path
   * @param scale the scaling factor for resolution (1 = normal, 2 = high res)
   * @throws IOException if file operations fail
   */
  public static void exportToPng(String mermaidContent, String outputPath, int scale)
      throws IOException {
    Path path = Paths.get(outputPath);
    if (path.getParent() != null) {
      Files.createDirectories(path.getParent());
    }

    try (Playwright playwright = Playwright.create()) {
      Browser browser =
          playwright
              .chromium()
              .launch(new BrowserType.LaunchOptions().setHeadless(true));

      // Create page with large viewport and device scale factor for high resolution
      Page page = browser.newPage(
          new Browser.NewPageOptions()
              .setViewportSize(3840, 2160)
              .setDeviceScaleFactor(scale));

      String html = buildHtmlContent(mermaidContent, scale);
      page.setContent(html);
      page.waitForSelector(".mermaid svg");

      // Wait for Mermaid to render the diagram
      page.waitForTimeout(2000);

      // Screenshot the diagram element only - this will include the device scale factor
      page.locator(".mermaid").screenshot(
          new Locator.ScreenshotOptions()
              .setPath(path));

      browser.close();
    }
  }

  /**
   * Builds an HTML document containing the Mermaid diagram.
   *
   * @param mermaidContent the Mermaid diagram content
   * @param scale the scaling factor for the diagram size
   * @return HTML string ready for rendering
   */
  private static String buildHtmlContent(String mermaidContent, int scale) {
    // Escape the diagram content for use in HTML
    String escapedContent = mermaidContent
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");

    return String.format(
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0" />
            <title>Mermaid Diagram</title>
            <script src="%s"></script>
            <style>
                * {
                    margin: 0;
                    padding: 0;
                    box-sizing: border-box;
                }
                body {
                    background-color: white;
                    font-family: Arial, sans-serif;
                    display: inline-block;
                }
                .mermaid {
                    display: block;
                    transform-origin: top left;
                }
            </style>
        </head>
        <body>
            <script>
                mermaid.initialize({ 
                    startOnLoad: true, 
                    theme: 'default',
                    maxTextSize: 90000,
                    securityLevel: 'loose'
                });
            </script>
            <div class="mermaid">
        %s
            </div>
        </body>
        </html>
        """,
        MERMAID_CDN, escapedContent);
  }
}


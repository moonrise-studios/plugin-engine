package gg.moonrise.engine.paper.dialog.canvas.internal;

import gg.moonrise.engine.paper.dialog.canvas.BedrockForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;

/**
 * Builds Cumulus forms, the format both Floodgate and Geyser send.
 * <p>
 * Internal; not part of the supported API. Only referenced once Floodgate or Geyser is known to be enabled.
 */
public final class CumulusForms {

    private CumulusForms() {
    }

    /**
     * Builds a simple form whose responses call the form's handlers.
     * @param form the form
     * @return the Cumulus form
     */
    public static SimpleForm simple(BedrockForm form) {
        SimpleForm.Builder builder = SimpleForm.builder()
                .title(form.title())
                .content(form.content());
        for (BedrockForm.Button button : form.buttons()) {
            BedrockForm.Image image = button.image();
            if (image == null) {
                builder.button(button.text());
            } else {
                builder.button(button.text(), image.url() ? FormImage.Type.URL : FormImage.Type.PATH, image.data());
            }
        }

        return builder.validResultHandler(response -> form.onClick().accept(response.clickedButtonId()))
                .closedOrInvalidResultHandler(form.onClose())
                .build();
    }
}

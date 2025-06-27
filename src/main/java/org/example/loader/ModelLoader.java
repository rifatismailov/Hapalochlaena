package org.example.loader;

import ai.djl.Application;
import ai.djl.ModelException;
import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;

@Component
public class ModelLoader {

    private static final String MODEL_NAME = "paraphrase-multilingual-MiniLM-L12-v2";
    private static final Path LOCAL_MODEL_DIR = Paths.get("models", MODEL_NAME);

    private ZooModel<String, float[]> model;

    @PostConstruct
    public void init() {
        try {
            if (!Files.exists(LOCAL_MODEL_DIR) || Files.list(LOCAL_MODEL_DIR).findAny().isEmpty()) {
                downloadAndCopyModel();
            }

            Criteria<String, float[]> criteria = Criteria.builder()
                    .optApplication(Application.NLP.TEXT_EMBEDDING)
                    .setTypes(String.class, float[].class)
                    .optEngine("PyTorch")
                    .optModelPath(LOCAL_MODEL_DIR)
                    .optTranslatorFactory(new TextEmbeddingTranslatorFactory())
                    .build();

            this.model = ModelZoo.loadModel(criteria);

        } catch (IOException | ModelException | TranslateException e) {
            throw new RuntimeException("Не вдалося ініціалізувати модель", e);
        }
    }

    public Predictor<String, float[]> newPredictor(){
        return model.newPredictor();
    }

    private void downloadAndCopyModel() throws IOException, ModelException, TranslateException {
        Criteria<String, float[]> downloadCriteria = Criteria.builder()
                .optApplication(Application.NLP.TEXT_EMBEDDING)
                .setTypes(String.class, float[].class)
                .optEngine("PyTorch")
                .optModelUrls("djl://ai.djl.huggingface.pytorch/sentence-transformers/" + MODEL_NAME)
                .optTranslatorFactory(new TextEmbeddingTranslatorFactory())
                .build();

        try (ZooModel<String, float[]> tempModel = ModelZoo.loadModel(downloadCriteria)) {
            Path downloadedPath = tempModel.getModelPath();
            copyDirectory(downloadedPath, LOCAL_MODEL_DIR);
        }
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source).forEach(src -> {
            try {
                Path dest = target.resolve(source.relativize(src));
                if (Files.isDirectory(src)) {
                    Files.createDirectories(dest);
                } else {
                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new RuntimeException("Не вдалося скопіювати: " + src, e);
            }
        });
    }
}

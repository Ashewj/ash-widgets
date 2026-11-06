package br.com.widgetpoco;

/** Registry for independently configurable widgets. Keep provider component IDs stable. */
final class WidgetCatalog {
    static final String TIM = "tim-internet";
    static final class Entry {
        final String id, title, description;
        final Class<?> provider;
        Entry(String id, String title, String description, Class<?> provider) {
            this.id=id; this.title=title; this.description=description; this.provider=provider;
        }
    }
    static final Entry[] ENTRIES = {
        new Entry(TIM, "Internet TIM", "Franquia, saldo e média diária", DataWidget.class)
    };
}

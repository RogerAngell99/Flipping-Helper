package flippinghelper;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.QuantityFormatter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class FlippingHelperPanel extends PluginPanel {

    private final ItemManager itemManager;
    private final Consumer<Integer> refreshCallback;
    private final Consumer<Integer> refreshPricesCallback;
    private final Runnable reloadAllCallback;
    private final Consumer<FlippingItem> hoverCallback;

    private final List<SuggestionRow> suggestionRows = new ArrayList<>();
    private final JPanel suggestionsContainer;
    private SuggestionRow selectedRow = null;
    private SuggestionRow hoveredRow = null;

    public FlippingHelperPanel(ItemManager itemManager, Consumer<Integer> refreshCallback,
                                Consumer<Integer> refreshPricesCallback, Runnable reloadAllCallback,
                                Consumer<FlippingItem> hoverCallback) {
        super();
        this.itemManager = itemManager;
        this.refreshCallback = refreshCallback;
        this.refreshPricesCallback = refreshPricesCallback;
        this.reloadAllCallback = reloadAllCallback;
        this.hoverCallback = hoverCallback;

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Adiciona botão de reload global no topo
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(new EmptyBorder(0, 0, 10, 0));

        JButton reloadAllButton = new JButton("Reload All Items");
        reloadAllButton.setToolTipText("Fetch fresh data from API and reload all items");
        reloadAllButton.addActionListener(e -> reloadAllCallback.run());
        headerPanel.add(reloadAllButton, BorderLayout.CENTER);

        add(headerPanel, BorderLayout.NORTH);

        suggestionsContainer = new JPanel();
        // GridLayout divide o espaço igualmente entre as 8 linhas
        suggestionsContainer.setLayout(new GridLayout(8, 1, 0, 5));

        // Cria 8 linhas de sugestões que preenchem todo o espaço disponível
        for (int i = 0; i < 8; i++) {
            SuggestionRow row = new SuggestionRow(i);
            suggestionRows.add(row);
            suggestionsContainer.add(row.getPanel());
        }

        JScrollPane scrollPane = new JScrollPane(suggestionsContainer);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        add(scrollPane, BorderLayout.CENTER);
    }

    public void updateSuggestions(List<FlippingItem> items) {
        for (int i = 0; i < suggestionRows.size(); i++) {
            if (i < items.size()) {
                suggestionRows.get(i).updateItem(items.get(i));
            } else {
                suggestionRows.get(i).clear();
            }
        }
    }

    public void updateSuggestion(int index, FlippingItem item) {
        if (index >= 0 && index < suggestionRows.size()) {
            suggestionRows.get(index).updateItem(item);
        }
    }

    private class SuggestionRow {
        // AJUSTE AQUI: Altura de cada retângulo em pixels (recomendado: 80-120)
        private static final int ROW_HEIGHT = 89;

        private final int index;
        private final JPanel panel;
        private final JLabel iconLabel;
        private final JLabel infoLabel;
        private final JButton nextItemButton;
        private final JButton refreshPricesButton;
        private boolean selected = false;
        private FlippingItem currentItem = null;

        public SuggestionRow(int index) {
            this.index = index;

            panel = new JPanel(new BorderLayout(4, 0));
            panel.setPreferredSize(new Dimension(0, ROW_HEIGHT));
            panel.setMinimumSize(new Dimension(0, ROW_HEIGHT));
            panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));
            panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                new EmptyBorder(5, 5, 5, 5)
            ));

            // Panel esquerdo com ícone maior
            iconLabel = new JLabel();
            iconLabel.setMinimumSize(new Dimension(36, 36));
            iconLabel.setPreferredSize(new Dimension(36, 36));
            iconLabel.setMaximumSize(new Dimension(36, 36));
            iconLabel.setVerticalAlignment(SwingConstants.TOP);

            // Panel central com informações distribuídas verticalmente
            JPanel infoPanel = new JPanel();
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

            infoLabel = new JLabel();
            infoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            infoPanel.add(Box.createVerticalGlue());
            infoPanel.add(infoLabel);
            infoPanel.add(Box.createVerticalGlue());

            // Panel com botões à direita
            JPanel buttonsPanel = new JPanel();
            buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));

            // Botão de refresh prices (↻)
            refreshPricesButton = new JButton("↻");
            refreshPricesButton.setFont(new Font("Dialog", Font.BOLD, 18));
            refreshPricesButton.setPreferredSize(new Dimension(30, 45));
            refreshPricesButton.setMaximumSize(new Dimension(30, 45));
            refreshPricesButton.setMargin(new Insets(0, 0, 0, 0));
            refreshPricesButton.setToolTipText("Refresh prices for this item");
            refreshPricesButton.addActionListener(e -> refreshPricesCallback.accept(index));

            // Botão de next item (»)
            nextItemButton = new JButton("»");
            nextItemButton.setFont(new Font("Dialog", Font.BOLD, 16));
            nextItemButton.setPreferredSize(new Dimension(30, 45));
            nextItemButton.setMaximumSize(new Dimension(30, 45));
            nextItemButton.setMargin(new Insets(0, 0, 0, 0));
            nextItemButton.setToolTipText("Show next item");
            nextItemButton.addActionListener(e -> refreshCallback.accept(index));

            buttonsPanel.add(refreshPricesButton);
            buttonsPanel.add(Box.createVerticalStrut(3));
            buttonsPanel.add(nextItemButton);

            panel.add(iconLabel, BorderLayout.WEST);
            panel.add(infoPanel, BorderLayout.CENTER);
            panel.add(buttonsPanel, BorderLayout.EAST);

            // Adiciona listener de clique ao painel
            panel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    handleRowClick();
                }
            });

            // Adiciona efeito hover e notifica o callback
            panel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    panel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    handleRowHover(true);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    panel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    handleRowHover(false);
                }
            });
        }

        private void handleRowClick() {
            // Deseleciona a linha anteriormente selecionada
            if (selectedRow != null && selectedRow != this) {
                selectedRow.setSelected(false);
            }

            // Seleciona esta linha
            setSelected(true);
            selectedRow = this;

            // Notifica o callback de hover com o item selecionado
            if (currentItem != null && hoverCallback != null) {
                hoverCallback.accept(currentItem);
            }
        }

        private void handleRowHover(boolean entered) {
            if (entered) {
                hoveredRow = this;
                // Notifica o callback quando o mouse entra
                if (currentItem != null && hoverCallback != null) {
                    hoverCallback.accept(currentItem);
                }
            } else {
                if (hoveredRow == this) {
                    hoveredRow = null;
                }
                // Quando o mouse sai, limpa o highlight apenas se não houver seleção
                if (selectedRow == null && hoverCallback != null) {
                    hoverCallback.accept(null);
                }
            }
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
            updateBorder();
        }

        private void updateBorder() {
            if (selected) {
                panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(255, 165, 0), 3), // Laranja
                    new EmptyBorder(5, 5, 5, 5)
                ));
            } else {
                panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.GRAY, 1),
                    new EmptyBorder(5, 5, 5, 5)
                ));
            }
        }

        public JPanel getPanel() {
            return panel;
        }

        public void updateItem(FlippingItem item) {
            this.currentItem = item;

            String action = item.getPredictedAction().equals("buy") ? "Buy" : "Sell";
            String itemName = item.getName();
            String quantity = String.valueOf(item.getQuantity());
            // CORRIGIDO: Buy = Low (comprar baixo), Sell = High (vender alto)
            String buyPrice = QuantityFormatter.formatNumber(item.getAdjustedLowPrice());
            String sellPrice = QuantityFormatter.formatNumber(item.getAdjustedHighPrice());

            String profitColor = ColorUtil.toHexColor(Color.GREEN);
            String profit = QuantityFormatter.formatNumber(item.getProfit());

            // Tudo em uma única label com espaçamento proporcional entre linhas
            // O nome do item é limitado a uma única linha com truncamento automático
            infoLabel.setText("<html><div style='line-height: 2.2; width: 150px;'>" +
                "<div style='white-space: nowrap; overflow: hidden; text-overflow: ellipsis;'>" +
                "<b>" + itemName + "</b></div>" +
                action + " " + quantity + "<br>" +
                "Buy: " + buyPrice + "<br>" +
                "Sell: " + sellPrice + "<br>" +
                "<font color='" + profitColor + "'>Profit: +" + profit + "</font>" +
                "</div></html>");

            try {
                int itemId = Integer.parseInt(item.getId());
                itemManager.getImage(itemId, item.getQuantity(), false).addTo(iconLabel);
                log.debug("Solicitada imagem assíncrona do item {} (ID: {}) para índice {}", item.getName(), itemId, index);
            } catch (NumberFormatException e) {
                iconLabel.setIcon(null);
                log.warn("Could not parse item ID: {}", item.getId());
            }

            panel.setVisible(true);
        }

        public void clear() {
            this.currentItem = null;
            infoLabel.setText("");
            iconLabel.setIcon(null);
            setSelected(false);
            panel.setVisible(false);
        }
    }
}

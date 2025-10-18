package flippinghelper;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.VarClientInt;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.VarClientIntChanged;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import java.awt.event.MouseEvent;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@PluginDescriptor(
		name = "Flipping Helper"
)
public class FlippingHelperPlugin extends Plugin
{
	@Inject
	private FlippingHelperConfig config;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ItemManager itemManager;

	@Inject
	private Client client;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private net.runelite.client.callback.ClientThread clientThread;

	@Inject
	private HighlightManager highlightManager;

	@Inject
	private GeMenuHandler menuHandler;

	@Inject
	private GeSearchAutoFillHandler searchAutoFillHandler;

	@Inject
	private GeInteractionHandler interactionHandler;

	private FlippingHelperPanel panel;
	private NavigationButton navButton;
	private final FlippingApiClient apiClient = new FlippingApiClient();
	private final MouseListener mouseListener = new OverlayMouseListener();
	private GeOfferAutoFillWidget autoFillWidget = null;

	private static final int MAX_SUGGESTIONS = 8;
	private static final long COOLDOWN_MILLIS = 5 * 60 * 1000; // 5 minutos

	private List<FlippingItem> allItems = new ArrayList<>();
	private List<FlippingItem> currentSuggestions = new ArrayList<>();
	private Map<String, Long> cooldownMap = new HashMap<>(); // itemId -> timestamp quando foi colocado em cooldown
	private boolean panelWasVisible = false;

	@Override
	protected void startUp() throws Exception
	{
		panel = new FlippingHelperPanel(
			itemManager,
			this::refreshSuggestion,
			this::refreshItemPrices,
			this::reloadAllItems,
			this::handleItemHover
		);
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/icon.png");
		navButton = NavigationButton.builder()
				.tooltip("Flipping Helper")
				.icon(icon)
				.priority(5)
				.panel(panel)
				.build();

		clientToolbar.addNavigation(navButton);

		// Register mouse listener for overlay interactions
		mouseManager.registerMouseListener(mouseListener);

		// Initialize the highlight overlay system
		highlightManager.initialize();

		// Adiciona listener para detectar quando o painel fica visível
		panel.addComponentListener(new java.awt.event.ComponentAdapter() {
			@Override
			public void componentShown(java.awt.event.ComponentEvent e) {
				log.debug("Painel ficou visível");
				if (!currentSuggestions.isEmpty() && !panelWasVisible) {
					panelWasVisible = true;
					log.info("Painel visível pela primeira vez, atualizando itens");
					SwingUtilities.invokeLater(() -> {
						panel.updateSuggestions(currentSuggestions);
						panel.revalidate();
						panel.repaint();
					});
				}
			}
		});

		// Busca os itens imediatamente ao iniciar o plugin
		fetchAndDisplayItems();
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientToolbar.removeNavigation(navButton);
		mouseManager.unregisterMouseListener(mouseListener);
		highlightManager.shutdown();
	}

	/**
	 * Handle item hover/selection from the panel.
	 * Updates the highlight manager to show GE overlays and sets the auto-fill item.
	 * Must be called on client thread to avoid threading issues.
	 */
	private void handleItemHover(FlippingItem item) {
		// Schedule on client thread to avoid "must be called on client thread" errors
		clientThread.invokeLater(() -> {
			if (item != null) {
				log.debug("Item hovered: {} ({})", item.getName(), item.getId());
				highlightManager.setCurrentItem(item);
				searchAutoFillHandler.setCurrentItem(item);
			} else {
				log.debug("Item hover cleared");
				highlightManager.clearCurrentItem();
				searchAutoFillHandler.clearCurrentItem();
			}
		});
	}

	/**
	 * Update highlights on every game tick to keep them in sync with the GE state.
	 * Also check for GE search interface opening.
	 */
	@Subscribe
	public void onGameTick(GameTick event) {
		highlightManager.redraw();
		searchAutoFillHandler.tick();
	}

	/**
	 * Handle menu entries being added to inject custom options.
	 */
	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event) {
		menuHandler.onMenuEntryAdded(event);
	}

	/**
	 * Handle menu options being clicked for custom actions.
	 */
	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event) {
		menuHandler.onMenuOptionClicked(event);
	}

	/**
	 * Handle VarClientInt changes to detect chatbox state changes.
	 */
	@Subscribe
	public void onVarClientIntChanged(VarClientIntChanged event) {
		// Check if chatbox input type changed (7 = text input for GE)
		if (event.getIndex() != VarClientInt.INPUT_TYPE) {
			return;
		}

		int inputType = client.getVarcIntValue(VarClientInt.INPUT_TYPE);

		// Input type 7 = chatbox text input (price/quantity)
		if (inputType == 7) {
			// Schedule on client thread to ensure widgets are available
			clientThread.invokeLater(() -> {
				Widget chatboxContainer = client.getWidget(ComponentID.CHATBOX_CONTAINER);
				if (chatboxContainer == null) {
					return;
				}

				// Create the auto-fill widget
				autoFillWidget = new GeOfferAutoFillWidget(client, interactionHandler, chatboxContainer);

				// Check what we're setting and show appropriate text
				FlippingItem currentItem = highlightManager.getCurrentItem();
				if (currentItem == null) {
					return;
				}

				if (interactionHandler.isSettingQuantity()) {
					autoFillWidget.showQuantity(currentItem.getQuantity());
				} else if (interactionHandler.isSettingPrice()) {
					int price = getPriceForItem(currentItem);
					autoFillWidget.showPrice(price);
				}
			});
		}
	}

	/**
	 * Get the appropriate price for the current item based on its action.
	 */
	private int getPriceForItem(FlippingItem item) {
		if ("buy".equals(item.getPredictedAction())) {
			return (int) item.getAdjustedLowPrice();
		} else {
			return (int) item.getAdjustedHighPrice();
		}
	}

	private void fetchAndDisplayItems() {
		new SwingWorker<List<FlippingItem>, Void>() {
			@Override
			protected List<FlippingItem> doInBackground() throws Exception {
				log.info("Fetching items from API...");
				List<FlippingItem> fetchedItems = apiClient.getItems();
				log.info("API returned {} items", fetchedItems.size());

				if (fetchedItems.isEmpty()) {
					log.warn("No items received from API!");
					return new ArrayList<>();
				}

				// Log sample of first item for debugging
				if (!fetchedItems.isEmpty()) {
					FlippingItem sample = fetchedItems.get(0);
					log.info("Sample item: {} - Price: {}, Profit: {}, Volume: {}, Score: {}, Dump: {}",
						sample.getName(),
						sample.getAdjustedLowPrice(),
						sample.getProfit(),
						sample.getDailyVolume(),
						sample.getScore(),
						sample.getDumpSignalScore());
				}

				// Count items with dump signals
				long dumpItemCount = fetchedItems.stream()
					.filter(item -> item.getDumpSignalScore() != null && item.getDumpSignalScore() > 0)
					.count();
				log.info("Items with dump signals: {} out of {}", dumpItemCount, fetchedItems.size());

				// Log a few items with dump signals if any exist
				fetchedItems.stream()
					.filter(item -> item.getDumpSignalScore() != null && item.getDumpSignalScore() > 0)
					.limit(3)
					.forEach(item -> log.info("Dump item found: {} (ID: {}) - Dump Score: {}, Reasons: {}, Peak Price: {}",
						item.getName(),
						item.getId(),
						item.getDumpSignalScore(),
						item.getDumpSignalReasons(),
						item.getDumpPeakPrice()));

				// Apply filters from config
				allItems = filterItems(fetchedItems);

				// Ordena por score descendente
				allItems.sort(Comparator.comparing(FlippingItem::getScore).reversed());

				log.info("Final result: {} items to display (from {} fetched, {} after filtering)",
					Math.min(MAX_SUGGESTIONS, allItems.size()), fetchedItems.size(), allItems.size());

				return selectTopSuggestions();
			}

			@Override
			protected void done() {
				try {
					currentSuggestions = get();
					if (!currentSuggestions.isEmpty()) {
						log.info("Obtidos {} itens únicos", currentSuggestions.size());

						// IMPORTANTE: Toda atualização de UI deve ser feita no EDT
						SwingUtilities.invokeLater(() -> {
							panel.updateSuggestions(currentSuggestions);
							panel.revalidate();
							panel.repaint();
						});
					}
				} catch (Exception e) {
					log.error("Erro ao buscar ou exibir itens", e);
				}
			}
		}.execute();
	}

	/**
	 * Apply filters from config to the list of items.
	 * Package-private for testing.
	 */
	List<FlippingItem> filterItems(List<FlippingItem> items) {
		// Log current filter settings
		log.info("=== Filter Settings ===");
		log.info("Min Buy Price: {}", config.minBuyPrice());
		log.info("Max Buy Price: {}", config.maxBuyPrice());
		log.info("Min Profit: {}", config.minProfit());
		log.info("Min Daily Volume: {}", config.minDailyVolume());
		log.info("Min Score: {}", config.minScore());
		log.info("Dump Filter: {}", config.dumpFilter());
		log.info("======================");

		List<FlippingItem> filtered = items.stream()
			.filter(item -> {
				boolean passes = passesFilters(item);
				if (!passes) {
					log.debug("Item filtered out: {} - Price: {}, Profit: {}, Volume: {}, Score: {}, Dump: {}",
						item.getName(),
						item.getAdjustedLowPrice(),
						item.getProfit(),
						item.getDailyVolume(),
						item.getScore(),
						item.getDumpSignalScore());
				}
				return passes;
			})
			.collect(Collectors.toList());

		log.info("Items after filtering: {} out of {}", filtered.size(), items.size());
		return filtered;
	}

	/**
	 * Check if an item passes all configured filters.
	 * Package-private for testing.
	 */
	boolean passesFilters(FlippingItem item) {
		// Minimum buy price filter
		int minBuyPrice = config.minBuyPrice();
		if (minBuyPrice > 0 && item.getAdjustedLowPrice() < minBuyPrice) {
			return false;
		}

		// Maximum buy price filter
		int maxBuyPrice = config.maxBuyPrice();
		if (maxBuyPrice > 0 && item.getAdjustedLowPrice() > maxBuyPrice) {
			return false;
		}

		// Minimum profit filter
		int minProfit = config.minProfit();
		if (minProfit > 0 && item.getProfit() < minProfit) {
			return false;
		}

		// Minimum daily volume filter
		int minDailyVolume = config.minDailyVolume();
		if (minDailyVolume > 0 && item.getDailyVolume() < minDailyVolume) {
			return false;
		}

		// Minimum score filter
		double minScore = config.minScore();
		if (minScore > 0 && item.getScore() < minScore) {
			return false;
		}

		// Dump filter
		FlippingHelperConfig.DumpFilter dumpFilter = config.dumpFilter();
		boolean isDumpItem = item.getDumpSignalScore() != null && item.getDumpSignalScore() > 0;

		switch (dumpFilter) {
			case DUMP_ONLY:
				if (!isDumpItem) {
					return false;
				}
				break;
			case NO_DUMP:
				if (isDumpItem) {
					return false;
				}
				break;
			case ALL:
			default:
				// No filtering based on dump status
				break;
		}

		return true;
	}

	private List<FlippingItem> selectTopSuggestions() {
		cleanExpiredCooldowns();

		Set<String> displayedIds = currentSuggestions.stream()
				.map(FlippingItem::getId)
				.collect(Collectors.toSet());

		return allItems.stream()
				.filter(item -> !cooldownMap.containsKey(item.getId())) // Filtra itens em cooldown
				.filter(item -> !displayedIds.contains(item.getId())) // Filtra itens já sendo exibidos
				.limit(MAX_SUGGESTIONS)
				.collect(Collectors.toList());
	}

	private void cleanExpiredCooldowns() {
		long now = System.currentTimeMillis();
		cooldownMap.entrySet().removeIf(entry -> (now - entry.getValue()) >= COOLDOWN_MILLIS);
	}

	private void refreshSuggestion(int index) {
		if (index < 0 || index >= currentSuggestions.size()) {
			return;
		}

		FlippingItem oldItem = currentSuggestions.get(index);
		// Adiciona o item antigo ao cooldown
		cooldownMap.put(oldItem.getId(), System.currentTimeMillis());
		log.info("Item {} (ID: {}) adicionado ao cooldown por 5 minutos", oldItem.getName(), oldItem.getId());

		// Busca um novo item que não está em cooldown e não está sendo exibido
		cleanExpiredCooldowns();
		Set<String> displayedIds = currentSuggestions.stream()
				.map(FlippingItem::getId)
				.collect(Collectors.toSet());

		Optional<FlippingItem> newItem = allItems.stream()
				.filter(item -> !cooldownMap.containsKey(item.getId()))
				.filter(item -> !displayedIds.contains(item.getId()))
				.findFirst();

		if (newItem.isPresent()) {
			currentSuggestions.set(index, newItem.get());
			log.info("Substituindo item no índice {} por: {} (ID: {})", index, newItem.get().getName(), newItem.get().getId());

			SwingUtilities.invokeLater(() -> {
				panel.updateSuggestion(index, newItem.get());
				panel.revalidate();
				panel.repaint();
			});
		} else {
			log.warn("Nenhum item disponível para substituir o índice {}", index);
		}
	}

	private void refreshItemPrices(int index) {
		if (index < 0 || index >= currentSuggestions.size()) {
			return;
		}

		FlippingItem currentItem = currentSuggestions.get(index);
		String itemId = currentItem.getId();
		log.info("Atualizando preços para o item {} (ID: {})", currentItem.getName(), itemId);

		new SwingWorker<FlippingItem, Void>() {
			@Override
			protected FlippingItem doInBackground() throws Exception {
				// Busca dados atualizados da API
				List<FlippingItem> freshItems = apiClient.getItems();
				// Encontra o item específico pelo ID
				return freshItems.stream()
						.filter(item -> item.getId().equals(itemId))
						.findFirst()
						.orElse(null);
			}

			@Override
			protected void done() {
				try {
					FlippingItem updatedItem = get();
					if (updatedItem != null) {
						// Atualiza o item na lista atual
						currentSuggestions.set(index, updatedItem);
						// Atualiza também na lista completa se existir
						for (int i = 0; i < allItems.size(); i++) {
							if (allItems.get(i).getId().equals(itemId)) {
								allItems.set(i, updatedItem);
								break;
							}
						}

						log.info("Preços atualizados para {}: Buy={}, Sell={}, Profit={}",
								updatedItem.getName(),
								updatedItem.getAdjustedLowPrice(),
								updatedItem.getAdjustedHighPrice(),
								updatedItem.getProfit());

						SwingUtilities.invokeLater(() -> {
							panel.updateSuggestion(index, updatedItem);
							panel.revalidate();
							panel.repaint();
						});
					} else {
						log.warn("Item com ID {} não encontrado na atualização da API", itemId);
					}
				} catch (Exception e) {
					log.error("Erro ao atualizar preços do item {}", itemId, e);
				}
			}
		}.execute();
	}

	private void reloadAllItems() {
		log.info("Recarregando todos os itens da API...");
		// Limpa o cooldown ao recarregar tudo
		cooldownMap.clear();
		fetchAndDisplayItems();
	}

	@Provides
	FlippingHelperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FlippingHelperConfig.class);
	}

	/**
	 * Mouse listener for overlay interactions.
	 */
	private class OverlayMouseListener implements MouseListener {
		@Override
		public MouseEvent mouseClicked(MouseEvent event) {
			Point mousePos = client.getMouseCanvasPosition();
			if (highlightManager.handleMouseClick(mousePos)) {
				event.consume();
			}
			return event;
		}

		@Override
		public MouseEvent mousePressed(MouseEvent event) {
			return event;
		}

		@Override
		public MouseEvent mouseReleased(MouseEvent event) {
			return event;
		}

		@Override
		public MouseEvent mouseEntered(MouseEvent event) {
			return event;
		}

		@Override
		public MouseEvent mouseExited(MouseEvent event) {
			return event;
		}

		@Override
		public MouseEvent mouseDragged(MouseEvent event) {
			return event;
		}

		@Override
		public MouseEvent mouseMoved(MouseEvent event) {
			Point mousePos = client.getMouseCanvasPosition();
			highlightManager.handleMouseMove(mousePos);
			return event;
		}
	}
}
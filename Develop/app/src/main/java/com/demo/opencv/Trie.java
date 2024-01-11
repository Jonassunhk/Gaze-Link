package com.demo.opencv;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Trie {
    private final TrieNode root;

    public Trie() {
        root = new TrieNode();
    }

    public void addString(String word) {
        TrieNode current = root;
        for (char c : word.toCharArray()) {
            assert current != null;
            if (!current.getChildren().containsKey(c)) {
                current.getChildren().put(c, new TrieNode());
            }
            current = current.getChildren().get(c);
        }
        assert current != null;
        current.setEndOfWord(true);
    }

    public void removeString(String word) {
        removeString(root, word, 0);
    }

    private boolean removeString(TrieNode current, String word, int index) {
        if (index == word.length()) {
            if (!current.isEndOfWord()) {
                return false; // Word doesn't exist in the Trie
            }
            current.setEndOfWord(false);
            return current.getChildren().isEmpty();
        }

        char c = word.charAt(index);
        if (!current.getChildren().containsKey(c)) {
            return false; // Word doesn't exist in the Trie
        }

        boolean shouldDeleteCurrentNode = removeString(current.getChildren().get(c), word, index + 1);

        if (shouldDeleteCurrentNode) {
            current.getChildren().remove(c);
            return current.getChildren().isEmpty();
        }

        return false;
    }

    public List<String> searchMatchingStrings(String prefix) {
        List<String> matchingStrings = new ArrayList<>();
        TrieNode current = root;

        for (char c : prefix.toCharArray()) {
            assert current != null;
            if (!current.getChildren().containsKey(c)) {
                return matchingStrings; // No matching strings found
            }
            current = current.getChildren().get(c);
        }

        assert current != null;
        searchMatchingStrings(current, prefix, matchingStrings);
        return matchingStrings;
    }

    private void searchMatchingStrings(TrieNode node, String prefix, List<String> matchingStrings) {
        if (node.isEndOfWord()) {
            matchingStrings.add(prefix);
        }

        for (char c : node.getChildren().keySet()) {
            searchMatchingStrings(Objects.requireNonNull(node.getChildren().get(c)), prefix + c, matchingStrings);
        }
    }
}

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;

public class FileListMaker
{
    // ---- Program state (kept in main's scope and passed to the helpers) ----
    // list              : the list currently in memory
    // currentFileName   : name (with .txt) of the file the list came from / was saved to, or "" if none yet
    // needsToBeSaved    : the "dirty" flag; true when the list has changes not yet on disk

    public static void main(String[] args)
    {
        Scanner in = new Scanner(System.in);
        ArrayList<String> list = new ArrayList<>();
        String currentFileName = "";
        boolean needsToBeSaved = false;
        boolean done = false;
        String cmd;

        do
        {
            displayList(list, currentFileName, needsToBeSaved);
            cmd = SafeInput.getRegExString(in,
                    "A-Add  D-Delete  I-Insert  M-Move  V-View  C-Clear  O-Open  S-Save  Q-Quit\nChoose",
                    "[AaDdIiMmVvCcOoSsQq]").toUpperCase();

            // All file operations happen inside this try block.
            // The static support methods just throw; main handles the problems.
            try
            {
                switch (cmd)
                {
                    case "A":
                        list.add(SafeInput.getNonZeroLenString(in, "Enter the item to add"));
                        needsToBeSaved = true;
                        break;

                    case "D":
                        if (list.isEmpty())
                        {
                            System.out.println("The list is empty; nothing to delete.");
                        }
                        else
                        {
                            int delNum = SafeInput.getRangedInt(in, "Enter the number of the item to delete", 1, list.size());
                            System.out.println("Deleted: " + list.remove(delNum - 1));
                            needsToBeSaved = true;
                        }
                        break;

                    case "I":
                        int insNum = SafeInput.getRangedInt(in, "Enter the position to insert at", 1, list.size() + 1);
                        String newItem = SafeInput.getNonZeroLenString(in, "Enter the item to insert");
                        list.add(insNum - 1, newItem);
                        needsToBeSaved = true;
                        break;

                    case "M":
                        if (list.size() < 2)
                        {
                            System.out.println("You need at least two items to move one.");
                        }
                        else
                        {
                            int from = SafeInput.getRangedInt(in, "Enter the number of the item to move", 1, list.size());
                            int to = SafeInput.getRangedInt(in, "Enter the position to move it to", 1, list.size());
                            if (from == to)
                            {
                                System.out.println("The item is already there.");
                            }
                            else
                            {
                                String moving = list.remove(from - 1);
                                list.add(to - 1, moving);
                                needsToBeSaved = true;
                            }
                        }
                        break;

                    case "V":
                        viewList(list);
                        break;

                    case "C":
                        if (list.isEmpty())
                        {
                            System.out.println("The list is already empty.");
                        }
                        else
                        {
                            list.clear();
                            needsToBeSaved = true;
                            System.out.println("List cleared.");
                        }
                        break;

                    case "S":
                        if (currentFileName.isEmpty())
                        {
                            currentFileName = askForFileName(in);
                        }
                        saveFile(list, currentFileName);
                        needsToBeSaved = false;
                        System.out.println("Saved to " + currentFileName);
                        break;

                    case "O":
                        // Never lose data: offer to save a dirty list before replacing it
                        if (needsToBeSaved)
                        {
                            if (SafeInput.getYNConfirm(in, "The current list has unsaved changes. Save it first?"))
                            {
                                if (currentFileName.isEmpty())
                                {
                                    currentFileName = askForFileName(in);
                                }
                                saveFile(list, currentFileName);
                                needsToBeSaved = false;
                                System.out.println("Saved to " + currentFileName);
                            }
                            else if (!SafeInput.getYNConfirm(in, "Abandon the unsaved changes and continue opening a file?"))
                            {
                                break; // user changed their mind; keep the current list
                            }
                        }
                        String openName = askForFileName(in);
                        // Load into a new list first so a failed load can't wipe the current one
                        ArrayList<String> loaded = loadFile(openName);
                        list = loaded;
                        currentFileName = openName;
                        needsToBeSaved = false; // freshly loaded lists are clean
                        System.out.println("Loaded " + list.size() + " item(s) from " + currentFileName);
                        viewList(list); // show the contents right after loading
                        break;

                    case "Q":
                        if (needsToBeSaved)
                        {
                            if (SafeInput.getYNConfirm(in, "The current list has unsaved changes. Save before quitting?"))
                            {
                                if (currentFileName.isEmpty())
                                {
                                    currentFileName = askForFileName(in);
                                }
                                saveFile(list, currentFileName);
                                needsToBeSaved = false;
                                System.out.println("Saved to " + currentFileName);
                                done = true;
                            }
                            else
                            {
                                done = SafeInput.getYNConfirm(in, "Quit WITHOUT saving? Your changes will be lost");
                            }
                        }
                        else
                        {
                            done = true;
                        }
                        break;
                }
            }
            catch (IOException e)
            {
                // Covers NoSuchFileException (a FileNotFoundException-style problem), permissions, disk errors, etc.
                System.out.println("\nFile error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                System.out.println("Your list in memory has not been changed.");
            }

        } while (!done);

        System.out.println("\nGoodbye!");
    }

    // ---- Static support methods ----

    /** Asks for a base name and returns it with .txt appended (adds nothing if they typed .txt already). */
    private static String askForFileName(Scanner in)
    {
        String base = SafeInput.getRegExString(in, "Enter the file's base name (no extension needed)", "[\\w\\- ]+(\\.txt)?");
        if (base.toLowerCase().endsWith(".txt"))
        {
            return base;
        }
        return base + ".txt";
    }

    /** Writes every item on its own line. Overwrites the file if it exists. */
    public static void saveFile(ArrayList<String> list, String fileName) throws IOException
    {
        Path file = Paths.get(fileName);
        Files.write(file, list, StandardCharsets.UTF_8);
    }

    /** Reads a list file into a new ArrayList, one item per line. */
    public static ArrayList<String> loadFile(String fileName) throws IOException
    {
        Path file = Paths.get(fileName);
        return new ArrayList<>(Files.readAllLines(file, StandardCharsets.UTF_8));
    }

    private static void viewList(ArrayList<String> list)
    {
        System.out.println("\n--------------------------------");
        if (list.isEmpty())
        {
            System.out.println("(the list is empty)");
        }
        for (int i = 0; i < list.size(); i++)
        {
            System.out.printf("%3d  %s%n", i + 1, list.get(i));
        }
        System.out.println("--------------------------------");
    }

    private static void displayList(ArrayList<String> list, String fileName, boolean dirty)
    {
        System.out.println("\n=== File: " + (fileName.isEmpty() ? "(new, unnamed list)" : fileName)
                + " | Items: " + list.size()
                + (dirty ? " | UNSAVED CHANGES" : "") + " ===");
    }
}
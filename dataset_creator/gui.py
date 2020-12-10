#!/usr/bin/env python3.8

"""
 Copyright (C) 2020  Hugo JOBY

     This program is free software: you can redistribute it and/or modify
     it under the terms of the GNU General Public License as published by
     the Free Software Foundation, either version 3 of the License, or
     (at your option) any later version.

     This program is distributed in the hope that it will be useful,
     but WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
     GNU General Public License for more details.

     You should have received a copy of the GNU General Public License
     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 """

import os
import copy
import io
from os import name, path
import threading
import time
from tkinter.constants import TRUE
from tkinter.filedialog import test
from typing import Type, final
from searchEngineRequest import SearchEngineRequest
import sys
import pygubu
import tkinter as tk
import logging
import webbrowser
import re

from tkinter import filedialog as fd
from tkinter import messagebox as mb

PROJECT_PATH = os.path.dirname(__file__)
PROJECT_UI = os.path.join(PROJECT_PATH, "uiDatasetCreator.ui")

FILENAME_COL = "Filename"
COMPLETENESS_COL = "Complete"
ISFRAMEWORK_COL = "Framework"
AUTOMODE_THREAD_ID = "AutoModeThread"


def huge_sanitization(text):
    # Remove non-word character
    text = re.sub("[^a-zA-Z\\s:,\\.]", "", text)

    # Add space between dot
    text = re.sub("\\.{2,}", ".", text)

    # Remove unecessary space ( beginning of sentences + double space)
    text = re.sub("\\s\\s+", " ", text)
    text = re.sub("^\\s", "", text)

    # Replace carriage return
    text = text.replace('\n', " ").replace("\t", " ")

    return text


class TextRedirector(object):
    def __init__(self, widget, tag="stdout"):
        self.widget = widget
        self.tag = tag

    def write(self, str):
        self.widget.configure(state="normal")
        self.widget.insert("end", str, (self.tag,))
        self.widget.configure(state="disabled")


class AutomodeJob(threading.Thread):
    def __init__(self, parent_gui, *args, **keywords):
        threading.Thread.__init__(self, *args, **keywords)
        self.__stopped = threading.Event()
        self.__parent = parent_gui

    def run(self):
        print("Auto mode launched on {} files..".format(len(self.__parent.file_list)))
        var = self.__parent.file_list[self.__parent.current_index[0]]

        # C heck if additional keyword parameter is on
        keywords_bool = self.__parent.builder.get_variable("keywords_bool")

        start = self.__parent.current_index[0]
        for i in range(start, len(self.__parent.file_list)):
            if self.__stopped.is_set():
                break

            try:

                # Update current index , parent index and tree view in GUI
                self.__parent.current_index[0] = i
                self.__parent.update_tree_index()

                current_file = self.__parent.file_list[i]

                print("TREATING NOW ", current_file)

                famework_name = current_file[FILENAME_COL].replace("_", " ").replace(".req", "")

                if current_file[COMPLETENESS_COL] == "True" or current_file[COMPLETENESS_COL] == True:
                    logging.info("Skipping {}, already processed.".format(famework_name))
                    continue

                # Print current index
                process_print = "Processing '{}'.".format(famework_name)
                print(process_print)
                logging.info(process_print)

                # Code here
                se = SearchEngineRequest(famework_name)
                res = se.parse_google(keywords_bool)

                # Flag as complete if the query returned something
                self.__parent.file_list[i][COMPLETENESS_COL] = (len(res) != 0)

                filename_selected = current_file[FILENAME_COL]
                file_path = self.__parent.working_directory + "/" + filename_selected

                created = not path.exists(file_path)
                with  io.open(file_path, "a", encoding="utf-8") as f:
                    try:
                        if created:
                            if current_file[ISFRAMEWORK_COL]:
                                f.write("1")
                            else:
                                f.write("0")
                        logging.info("Writing {} characters to '{}' file".format(len(res), file_path))
                        f.write("\n" + res)
                        f.flush()
                    except StopIteration:
                        pass  # Ignore errors
            except Exception as e:
                logging.exception("An error occured during file parsing.", e)
                print("A file was skipped. Check the logs.")
                time.sleep(0.1)
        print("Auto mode done..")

    def stop(self):
        self.__stopped.set()

    def stopped(self):
        return self.__stopped.is_set()


class GUIDatasetCreator:

    def __init__(self):
        root = tk.Tk()

        self.builder = builder = pygubu.Builder()
        builder.add_resource_path(PROJECT_PATH)
        builder.add_from_file(PROJECT_UI)
        self.mainwindow = builder.get_object('mainFrame')
        builder.connect_callbacks(self)

        self.actual_dti_file = ""
        self.file_list = []
        self.working_file_list = []

        self.working_directory = ""
        # self.dt_index_file

        # Init components
        self.tree = builder.get_object('treeviewFileView')
        self.tree["columns"] = ("completeness", "framework")
        self.tree.heading("#0", text="Filename")
        self.tree.heading("completeness", text="Complete")
        self.tree.heading("framework", text="Framework")
        self.tree.bind("<Double-1>", self.on_double_click_tree)

        scrollbar = builder.get_object('scrollbar_1')
        scrollbar.config(command=self.tree.yview)

        self.tree.configure(yscrollcommand=scrollbar.set)

        self.file_counter = builder.get_variable('fileCounter')
        self.file_counter.set("0/0")

        self.treat_counter = builder.get_variable('toTreateLabel')
        self.treat_counter.set("0/0")

        keywords_bool = builder.get_variable("keywords_bool")
        keywords_bool.set(True)

        self.text_editor = builder.get_object('textFileEditor')

        # Mutable var, to allow passing it by reference, so bg jobs can modify it
        self.current_index = [0]
        self.current_elem = ""
        self.current_framework_name = ""

        self.filename_label = builder.get_variable('filenameLabelValue')
        self.filename_label.set("")

        self.framework_label = builder.get_variable('labelsFrameworkValue')
        self.framework_label.set("")

        root.bind('<Control-s>', self.save_current_file_call)
        root.bind('<Up>', self.get_previous_file)
        root.bind('<Down>', self.get_next_file)

        root.bind('<Left>', self.get_previous_file)
        root.bind('<Right>', self.get_next_file)

        self.is_current_framework = False

        self.console_output = builder.get_object('consoleOutput')
        sys.stdout = TextRedirector(self.console_output, "stdout")
        sys.stderr = TextRedirector(self.console_output, "stderr")

        # Async
        self.bg_threads = list()

    # Children class that will launch in bg the automode task. It needs to be stoppable.

    def __update_tree(self):
        self.tree.delete(*self.tree.get_children())

        filter_f = self.builder.get_variable('filterFile')
        if filter_f and filter_f.get():
            self.working_file_list = [elem for elem in self.file_list if elem[COMPLETENESS_COL] == "False"]
        else:
            self.working_file_list = copy.deepcopy(self.file_list)

        for index, item in enumerate(self.working_file_list):
            completeness = item[COMPLETENESS_COL]
            framework = item[ISFRAMEWORK_COL]
            self.tree.insert("", index, text=item[FILENAME_COL], values=(completeness, framework))
            index += 1

    def open_file(self):
        self.file_list = []  # Reset file list

        self.actual_dti_file = filename = fd.askopenfilename(filetypes=[("Dataset index", ".dti")],
                                                             title="Open a dataset index file.")
        print("Path of dataset index : ", filename)
        self.dt_index_file = open(filename, "r+")
        self.working_directory = os.path.dirname(filename)
        print("Working dir is now '%s'." % self.working_directory)

        to_check_c = 0
        for index, xline in enumerate(self.dt_index_file):
            try:
                san_split = xline.replace("\n", "").split(";")
                filename = san_split[0]
                completeness = san_split[1]
                is_framework = san_split[2]
                if (completeness == "False"): to_check_c += 1
                self.file_list.append(
                    {FILENAME_COL: filename, COMPLETENESS_COL: completeness, ISFRAMEWORK_COL: is_framework})
            except IndexError:
                print("Error on line ", index, ". Malformed data")

        logging.info("{} files were succesfully imported.".format(len(self.file_list)))

        self.working_file_list = copy.deepcopy(self.file_list)
        self.__update_tree()

        self.treat_counter.set("{}/{}".format(to_check_c, len(self.file_list)))
        self.update_current_file()

    def exit_program(self):
        sys.exit(0)

    def update_tree_index(self):
        children = self.tree.get_children()
        self.tree.focus(children[self.current_index[0]])
        self.tree.selection_set(children[self.current_index[0]])

    def update_current_file(self):
        self.file_counter.set("{}/{}".format(self.current_index[0] + 1, len(self.working_file_list)))
        filename_selected = self.working_file_list[self.current_index[0]][FILENAME_COL]
        file_path = self.working_directory + "/" + filename_selected

        self.current_framework_name = filename_selected.replace("_", " ").replace(".req", "")
        self.filename_label.set(self.current_framework_name)

        print("Value is ", self.working_file_list[self.current_index[0]])

        content = ""
        self.text_editor.delete('1.0', tk.END)

        created = not path.exists(file_path)

        with open(file_path, 'r+') as f:
            try:
                if created:
                    if self.working_file_list[self.current_index[0]][ISFRAMEWORK_COL] == "True":
                        f.write("1")
                    else:
                        f.write("0")
                else:
                    self.update_is_framework(f.read(1) == "1")
                    next(f)
                    content = f.read()
                    self.text_editor.insert('1.0', content)
            except StopIteration:
                pass  # Ignore errors

        # If the file is not empty anymore, mark as complete
        if len(content) != 0:
            self.file_list[self.current_index[0]][COMPLETENESS_COL] = True

        self.file_list[self.current_index[0]][ISFRAMEWORK_COL] = self.is_current_framework
        self.__update_tree()
        self.update_tree_index()

    def get_previous_file(self, event=None):
        self.current_index[0] -= 1
        if self.current_index[0] < 0:
            self.current_index[0] = 0
        self.update_current_file()

    def get_next_file(self, event=None):
        self.current_index[0] += 1
        if self.current_index[0] >= len(self.working_file_list):
            self.current_index[0] = len(self.working_file_list) - 1
            mb.showinfo("End reached", "You reached the last file. Congrats.")
        self.update_current_file()

    def on_double_click_tree(self, event):
        if not self.tree.selection():
            return

        item = self.tree.selection()[0]
        selected = self.tree.item(item, "text")
        self.current_index[0] = next(
            i for i, element in enumerate(self.working_file_list) if element[FILENAME_COL] == selected)
        self.update_current_file()

    def update_is_framework(self, value: bool):
        self.framework_label.set(value)
        self.is_current_framework = value
        self.file_list[self.current_index[0]][ISFRAMEWORK_COL] = value

    def toggle_framework_value(self):
        self.update_is_framework(not self.is_current_framework)

    def filter_file_pressed(self):
        self.__update_tree()

        # Ask the confirmation, before inserting result to the file

    def ask_confirmation_results(self, results: str) -> bool:
        cm = "Do you want to insert the following results ? \n {}".format(results)
        return mb.askyesno(title="Confirm the results", message=cm)

    # Launch query against Google Search
    def query_google(self, ask_confirmation=True):
        def wait_res():
            se = SearchEngineRequest(self.current_framework_name)
            res = se.parse_google()
            if ask_confirmation and self.ask_confirmation_results(res):
                self.text_editor.insert('1.0', "\n" + res)

        x = threading.Thread(target=wait_res)
        self.bg_threads.append(x)
        x.start()

    # Launch query against Duck Duck GO
    def query_duck_duck(self, ask_confirmation=True):
        def wait_res():
            se = SearchEngineRequest(self.current_framework_name)
            res = se.parse_duck_duckgo()
            if ask_confirmation and self.ask_confirmation_results(res):
                self.text_editor.insert('1.0', "\n" + res)

        x = threading.Thread(target=wait_res)
        self.bg_threads.append(x)
        x.start()

    # Launch against every known crawlers
    def bulk_request(self, ask_confirmation=True):
        def wait_res():
            se = SearchEngineRequest(self.current_framework_name)
            res = se.bulk()
            if ask_confirmation and self.ask_confirmation_results(res):
                self.text_editor.insert('1.0', "\n" + res)

        x = threading.Thread(target=wait_res)
        self.bg_threads.append(x)
        x.start()

    def open_web(self):
        print("Opening browser..")
        to_search = self.current_framework_name.replace("_", '+').replace(" ", "+")
        url = "https://google.com/search?q=%s&lr=lang_en" % to_search
        webbrowser.open(url, new=1)

    # Save current text to its related file
    def save_file(self):
        filename_selected = self.file_list[self.current_index[0]][FILENAME_COL]
        path = self.working_directory + "/" + filename_selected
        with open(path, "w") as file:
            byte = 0
            if self.is_current_framework: byte = 1
            file.write(str(byte) + "\n")
            file.write(self.text_editor.get(1.0, tk.END))

        self.__update_tree()

    def save_current_file_call(self, event=None):
        if self.file_list and self.file_list[self.current_index[0]]:
            self.save_file()

    def launch_auto_mode(self, event=None):
        # Clean background jobs
        self.bg_threads = [t for t in self.bg_threads if t.is_alive()]

        # Check if there is no Automode already launched
        if len([t for t in self.bg_threads if t.getName() == AUTOMODE_THREAD_ID]) > 0:
            print("Automode already launched..")
            print("Automode ", AUTOMODE_THREAD_ID)
            return

            # Launch new thread
        x = AutomodeJob(self)
        print("Thread name", x.getName())
        self.bg_threads.append(x)
        x.start()

        self.builder.get_object("stopAutoButton").state = "normal"
        # Activate stop button

    def stop_auto_mode(self):
        # Clean background jobs
        self.bg_threads = [t for t in self.bg_threads if t.is_alive()]

        # Check if there is no Automode already launched
        to_stop = [t for t in self.bg_threads if t.getName() == AUTOMODE_THREAD_ID]
        for t in to_stop:
            t.stop

    def save_dti(self):
        if len(self.actual_dti_file) == 0:
            raise ValueError("No .dti file to save")

        print("Saving")
        print(self.file_list)
        with open(self.actual_dti_file, "w") as dti_file:
            for f in self.file_list:
                dti_file.write(";".join([str(e) for e in f.values()]) + "\n")

    def generate_opennlp_file(self):

        extensions = [('Open NLP format', '*.txt')]
        dataset_path = fd.asksaveasfilename(filetypes=extensions, defaultextension=extensions)

        print("Saving at ", dataset_path)

        try:
            with open(dataset_path, "w", encoding='utf-8') as full_dataset:

                for file in self.file_list:
                    file_path = self.working_directory + "/" + file[FILENAME_COL]

                    if not path.exists(file_path):
                        continue

                    with open(file_path, 'r+') as f:
                        try:
                            tag = ""
                            if file[ISFRAMEWORK_COL] == "True":
                                tag = "Framework"
                            else:
                                tag = "NotFramework"

                            next(f)
                            content = huge_sanitization(str(f.read()))

                            if len(content) == 0:
                                continue

                            full_dataset.write(tag)
                            full_dataset.write("\t" + content + "\n")
                        except UnicodeDecodeError:
                            logging.error("An error occured riding file at {}".format(file_path))  # Ignore errors

        except IOError as e:
            print("Error trying to generate Open NLP dataset. Check the logs")
            logging.error("IOError trying to  save full dataset file.")
            logging.exception(e)

    def run(self):
        self.mainwindow.mainloop()

    def __del__(self):
        if self.dt_index_file:
            self.dt_index_file.close()

        # Terminate AutoJobs and other background processus
        if self.bg_threads:
            to_stop = [t for t in self.bg_threads if t.getName() == AUTOMODE_THREAD_ID]
            for t in to_stop:
                t.stop

            for t in self.bg_threads:
                t.join()

        if self.actual_dti_file:
            self.save_dti()


class Error(Exception):
    """Base class for exceptions in this module."""
    pass

class MissingFileError(Error):
    """Exception raised for errors missing files.

    Attributes:
        expression -- input expression in which the error occurred
        message -- explanation of the error
    """

    def __init__(self, expression, message : str):
        self.expression = expression
        self.message = message


class DirectoryCreationError(Error):
    """Exception raised for errors during directory creation.

    Attributes:
        expression -- input expression in which the error occurred
        message -- explanation of the error
    """

    def __init__(self, expression, message : str):
        self.expression = expression
        self.message = message
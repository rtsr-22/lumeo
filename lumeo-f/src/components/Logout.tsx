
import { useState, useRef, useEffect } from "react";

export default function Logout() {
  const [open, setOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const toggleDropdown = () => setOpen((prev) => !prev);

  // Close dropdown when clicking outside
  useEffect(() => {
    function handleClickOutside(event: MouseEvent): void {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  return (
    <> 
    <div className="relative hidden sm:block" ref={dropdownRef}>
      <img
        className="w-8 h-8 cursor-pointer"
        src="./src/assets/user-svgrepo-com.svg"
        alt="Profile"
        onClick={toggleDropdown}
      />

      
      {open && (
        <div className="absolute -right-2 mt-8 w-36 bg-white shadow-lg rounded-md p-2 border z-50">
          <button className="w-full text-left px-3 py-1 hover:bg-gray-100 rounded">
            Profile
          </button>
          <button className="w-full text-left px-3 py-1 hover:bg-gray-100 rounded text-red-600">
            Logout
          </button>
        </div>
      )}
    </div>

    
    </>
  );
}

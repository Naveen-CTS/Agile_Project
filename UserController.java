package com.cognizant.hi.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cognizant.hi.entity.AppointmentEntity;
import com.cognizant.hi.entity.UserEntity;
import com.cognizant.hi.model.Clinic;
import com.cognizant.hi.model.Doctor;
import com.cognizant.hi.model.User;
import com.cognizant.hi.model.UserLogin;
import com.cognizant.hi.service.AppointmentService;
import com.cognizant.hi.service.ClinicService;
import com.cognizant.hi.service.DoctorService;
import com.cognizant.hi.service.UserService;

@Controller
@SessionAttributes({ "user", "userLogin" })
public class UserController {
	@Autowired
	UserService userService;
	@Autowired
	DoctorService doctorService;
	@Autowired
	ClinicService clinicService;

	@Autowired
	AppointmentService appointmentService;

	// for the first time,container checks whether user attribute is exists in the
	// session.If not this method will be called,
	// user object created and stored in the session. For the next request, user
	// object will be retrieved from the syste

	@ModelAttribute("user")
	public User getUser() {
		return new User();
	}

	@ModelAttribute("userLogin")
	public UserLogin getUserLogin() {
		return new UserLogin();
	}

	@ModelAttribute("doctor")
	public Doctor getDoctor() {
		return new Doctor();
	}

	@ModelAttribute("clinic")
	public Clinic getClinic() {
		return new Clinic();
	}

	@RequestMapping(value = "/showregister", method = RequestMethod.GET)
	public String createRegistration(Model model) {
		model.addAttribute("user", new User());
		return "register";

	}

	@RequestMapping(value = "/saveuser", method = RequestMethod.POST)
	public String saveUser(@Valid @ModelAttribute("user") User user, BindingResult bresult, ModelMap model,
			RedirectAttributes redirectAttributes) {

		if (bresult.hasErrors()) {

			return "register";
		} else {
			boolean result = userService.saveUser(user);
			System.out.println("result=" + result);
			if (!result) {
				model.addAttribute("userId", "User already exists.");
				return "register";

			}

			return "NewUser";

		}
	}

	@RequestMapping(value = "/login", method = RequestMethod.GET)
	public String createLogin(Model model) {
		model.addAttribute("userLogin", new UserLogin());
		return "login";
		// return new ModelAndView("login", "command", new User());
	}

	@GetMapping(value = "/forgotUserId")
	public String forgetId(Model model) {
		return "forgotUserId";
	}

	@RequestMapping("/getUserId")
	public String getUserid(@RequestParam("contactNumber") String contactNumber,
			@RequestParam("securityQuestion") String question, @RequestParam("answer") String answer, Model model) {
		UserEntity user = userService.findByContactNumber(contactNumber);
		System.out.println(user);
		if (user == null) {
			model.addAttribute("message", "Your contact number is not registered with us ");
			return "forgotUserId";
		}

		if (question.equals(user.getSecurityQuestion()) && answer.equals(user.getAnswer())) {

			model.addAttribute("message", "Your User Id is : " + user.getUserid());
			return "forgotUserId";
		} else {
			model.addAttribute("message", "Invalid secret question credentials ");
			return "forgotUserId";
		}
	}

	// Forgot Password
	@GetMapping("/forgotPassword")
	public String forgotPassword(Model model) {
		return "forgotPassword";
	}

	@RequestMapping("/check")
	public String check(@RequestParam("userId") String userid, @RequestParam("securityQuestion") String question,
			@RequestParam("answer") String answer, Model model, HttpSession session) {
		UserEntity user = userService.findById(userid);
		if (user == null) {
			model.addAttribute("message", "Your UserId is not registered with us ");
			return "forgotPassword";
		}

		if (question.equals(user.getSecurityQuestion()) && answer.equals(user.getAnswer())) {
			session.setAttribute("username", user.getUserid());
			return "resetPassword";
		} else {
			model.addAttribute("message", "Invalid secret question credentials ");
			return "forgotPassword";
		}
	}

	@RequestMapping("/changePassword")
	public String resetPassword(@RequestParam("password") String password,
			@RequestParam("confirmationPassword") String cpassword, Model model, HttpSession session,
			RedirectAttributes redirectAttributes) {
		String userid = (String) session.getAttribute("username");
		UserEntity user = userService.findById(userid);
		if (password.equals(cpassword) && !password.equals(user.getPassword())) {
			boolean status = userService.updatePassword(user, password);
			if (status == true) {
				redirectAttributes.addFlashAttribute("uname", "Password changed Sucessfully");
				return "redirect:login";
			} else {
				redirectAttributes.addFlashAttribute("uname", "Password not changed, try after sometime");
				return "redirect:login";
			}
		} else if (password.equals(cpassword) && password.equals(user.getPassword())) {
			model.addAttribute("message", "Enter New password other than old password");
			return "resetPassword";
		} else {
			model.addAttribute("message", "new password and conformation are not same");
			return "resetPassword";
		}
	}

	@RequestMapping(value = "/home", method = RequestMethod.GET)
	public String home(@ModelAttribute("redirect") String redirect, RedirectAttributes redirectAttributes,
			@SessionAttribute(name = "userLogin", required = false) UserLogin usr, ModelMap model,
			@ModelAttribute("message") String message) {
		if (redirect != null && !redirect.equals("")) {
			if (redirect.equals("doctorHome")) {
				List<AppointmentEntity> appointments = appointmentService.fetchAllAppointments(usr.getUserid());
				if (appointments != null) {
					List<AppointmentEntity> pendingAppointments = new ArrayList<>();
					List<AppointmentEntity> approvedAppointments = new ArrayList<>();
					appointments.forEach((appointment) -> {
						if (appointment.getAppointmentStatus().equals("P"))
							pendingAppointments.add(appointment);
						else
							approvedAppointments.add(appointment);
					});
					model.addAttribute("pendingAppointments", pendingAppointments);
					model.addAttribute("approvedAppointments", approvedAppointments);
				}
				model.addAttribute("appointment", new AppointmentEntity());
			}

			else if (redirect.equals("patientHome")) {
				List<AppointmentEntity> patientAppointments = appointmentService
						.fetchPatientAppointments(usr.getUserid());
				if (patientAppointments != null) {
					List<AppointmentEntity> patAppAppointments = new ArrayList<>();
					List<AppointmentEntity> patRejAppointments = new ArrayList<>();
					patientAppointments.forEach((appointment) -> {
						if (appointment.getAppointmentStatus().equals("A"))
							patAppAppointments.add(appointment);
						else if (appointment.getAppointmentStatus().equals("R"))
							patRejAppointments.add(appointment);
					});
					System.out.println(patAppAppointments.size() + " appointments Accepted");
					System.out.println(patRejAppointments.size() + " appointments Rejected");
					model.addAttribute("approved", patAppAppointments.size());
					model.addAttribute("rejected", patRejAppointments.size());

				}
				model.addAttribute("appointment", new AppointmentEntity());
			}
			return redirect;

		}

		// user cannot enter into login pages by giving home url
		if (usr == null || usr.getUserid() == null || usr.getPassword() == null) {
			return "redirect:/login";
		}

		UserLogin user = usr;
		String result = "";
		String result1 = extracted(user, model, redirectAttributes, usr, result);
		if (result1.contains("Home"))
			model.addAttribute("message", message);
		if (result1.equals("doctorHome")) {
			List<AppointmentEntity> appointments = appointmentService.fetchAllAppointments(usr.getUserid());
			if (appointments != null) {
				List<AppointmentEntity> pendingAppointments = new ArrayList<>();
				List<AppointmentEntity> approvedAppointments = new ArrayList<>();
				appointments.forEach((appointment) -> {
					if (appointment.getAppointmentStatus().equals("P"))
						pendingAppointments.add(appointment);
					else
						approvedAppointments.add(appointment);
				});
				model.addAttribute("pendingAppointments", pendingAppointments);
				model.addAttribute("approvedAppointments", approvedAppointments);
			}
			model.addAttribute("appointment", new AppointmentEntity());
		} else if (result1.equals("patientHome")) {
			List<AppointmentEntity> patientAppointments = appointmentService.fetchPatientAppointments(usr.getUserid());
			if (patientAppointments != null) {
				List<AppointmentEntity> patAppAppointments = new ArrayList<>();
				List<AppointmentEntity> patRejAppointments = new ArrayList<>();
				patientAppointments.forEach((appointment) -> {
					if (appointment.getAppointmentStatus().equals("A"))
						patAppAppointments.add(appointment);
					else if (appointment.getAppointmentStatus().equals("R"))
						patRejAppointments.add(appointment);
				});
				System.out.println(patAppAppointments.size() + " appointments Accepted");
				System.out.println(patRejAppointments.size() + " appointments Rejected");
				model.addAttribute("approved", patAppAppointments.size());
				model.addAttribute("rejected", patRejAppointments.size());

			}
			model.addAttribute("appointment", new AppointmentEntity());
		}
		return result1;

	}

	@RequestMapping(value = "/validate", method = RequestMethod.POST)
	public String validateLogin(@Valid @ModelAttribute("userLogin") UserLogin user, BindingResult bresult,
			ModelMap model, RedirectAttributes redirectAttributes, @SessionAttribute("userLogin") UserLogin usr) {
		String result = "";
		if (bresult.hasErrors()) {

			return "login";
		}

		else {
			String result1 = extracted(user, model, redirectAttributes, usr, result);
			if (result1.contains("Home"))
				return "redirect:home";
			return "redirect:/" + result1;
		}

	}

	private String extracted(UserLogin user, ModelMap model, RedirectAttributes redirectAttributes, UserLogin usr,
			String result) {
		// System.out.println("method called");
		String user_category = userService.validateUser(user);
		System.out.println("category=" + user_category);
		if (user_category != null) {

			if (user_category.equals("Admin")) {

				result = "adminHome";
			}

			else if (user_category.equals("Doctor")) {
				result = "doctorHome";
			}

			else if (user_category.equals("Patient")) {
				result = "patientHome";
			}
			redirectAttributes.addFlashAttribute("redirect", result);
			return result;
		}

		else {
			redirectAttributes.addFlashAttribute("uname", "Invalid username/password");
			return "login";
		}
	}

}
